import logging
import random
import threading
import time

import ray

CHUNK_SIZE = 10
REPLICATION_FACTOR = 3

@ray.remote(concurrency_groups={"read_ops": 10, "write_ops": 2})
class NameNode:
    def __init__(self, storage_nodes: list):
        self.storage_nodes = storage_nodes.copy()
        self.metadata = {}
        self.locations = {}
        self.lock = threading.Lock()
        threading.Thread(target=self._replica_monitor, daemon=True).start()

    @ray.method(concurrency_group="write_ops")
    def upload(self, artifact_name: str, content: str) -> bool:
        with self.lock:
            self._prune_dead()
            chunks = [content[i : i + CHUNK_SIZE] for i in range(0, len(content), CHUNK_SIZE)]
            self.metadata[artifact_name] = len(chunks)
            rf = min(REPLICATION_FACTOR, len(self.storage_nodes))

            for cid, data in enumerate(chunks):
                self.locations[(artifact_name, cid)] = []
                candidates = self.storage_nodes.copy()
                while candidates:
                    node = candidates.pop(random.randrange(len(candidates)))
                    try:
                        ok = ray.get(node.store_chunk.remote(artifact_name, cid, data), timeout=5)
                        if ok:
                            self.locations[(artifact_name, cid)].append(node)
                            if len(self.locations[(artifact_name, cid)]) >= rf:
                                break
                    except Exception:
                        logging.warning(f"Failed storing chunk {cid} of {artifact_name} on {node}")

                if not self.locations[(artifact_name, cid)]:
                    logging.error(f"No replicas for chunk {cid} of {artifact_name}")
        return True

    @ray.method(concurrency_group="read_ops")
    def get(self, artifact_name: str) -> str | None:
        with self.lock:
            count = self.metadata.get(artifact_name)
            if count is None:
                return None
            parts = []
            for cid in range(count):
                found = False
                for node in self.locations.get((artifact_name, cid), []):
                    try:
                        ok, data = ray.get(node.get_chunk.remote(artifact_name, cid), timeout=1)
                        if ok:
                            parts.append(data)
                            found = True
                            break
                    except Exception:
                        continue
                if not found:
                    logging.warning(f"Chunk {cid} of {artifact_name} missing in all replicas")
            return "".join(parts)

    @ray.method(concurrency_group="write_ops")
    def update(self, artifact_name: str, content: str) -> bool:
        self.delete(artifact_name)
        return self.upload(artifact_name, content)

    @ray.method(concurrency_group="write_ops")
    def delete(self, artifact_name: str) -> bool:
        with self.lock:
            count = self.metadata.pop(artifact_name, None)
            if count is None:
                return False
            for cid in range(count):
                for node in self.locations.pop((artifact_name, cid), []):
                    try:
                        node.delete_artifact.remote(artifact_name)
                    except Exception:
                        logging.warning(f"Failed to delete {artifact_name} on {node}")
        return True

    def list_status(self) -> dict:
        with self.lock:
            self._prune_dead()
            status = {"artifacts": list(self.metadata.keys()), "locations": {}}
            for (name, cid), nodes in self.locations.items():
                idxs = [self.storage_nodes.index(n) for n in nodes if n in self.storage_nodes]
                status[f"{name}:{cid}"] = idxs
            return status

    def _prune_dead(self):
        alive, dead = [], set()
        for node in self.storage_nodes:
            try:
                ray.get(node.ping.remote(), timeout=1)
                alive.append(node)
            except Exception:
                dead.add(node)
                logging.warning(f"Pruned dead node {node}")
        self.storage_nodes = alive
        for key, nodes in list(self.locations.items()):
            self.locations[key] = [n for n in nodes if n not in dead]

    def _replica_monitor(self):
        while True:
            time.sleep(0.5)
            with self.lock:
                self._prune_dead()
                rf = min(REPLICATION_FACTOR, len(self.storage_nodes))
                for (artifact, cid), nodes in list(self.locations.items()):
                    alive_nodes = []
                    for node in nodes:
                        try:
                            ok, _ = ray.get(node.get_chunk.remote(artifact, cid), timeout=1)
                            if ok:
                                alive_nodes.append(node)
                        except Exception:
                            continue
                    if alive_nodes and len(alive_nodes) < rf:
                        data = None
                        for src in alive_nodes:
                            try:
                                ok, data = ray.get(src.get_chunk.remote(artifact, cid), timeout=2)
                                if ok and data is not None:
                                    break
                            except Exception:
                                continue
                        if data is None:
                            logging.warning(f"Lost data for {artifact}:{cid}")
                            continue
                        candidates = [n for n in self.storage_nodes if n not in alive_nodes]
                        for target in candidates[: rf - len(alive_nodes)]:
                            try:
                                success = ray.get(target.store_chunk.remote(artifact, cid, data), timeout=2)
                                if success:
                                    alive_nodes.append(target)
                            except Exception:
                                logging.warning(f"Replica write failed for {artifact}:{cid} on {target}")
                    self.locations[(artifact, cid)] = alive_nodes
