import random
import threading
import time
import ray

FAILURE_PROB = 0.00

@ray.remote(max_restarts=3, max_task_retries=5)
class StorageNode:
    def __init__(self, node_id: int):
        self.node_id = node_id
        self.store = {}
        threading.Thread(target=self._random_fail, daemon=True).start()

    def _random_fail(self):
        while True:
            time.sleep(5)
            if random.random() < FAILURE_PROB:
                ray.actor.exit_actor()

    def ping(self) -> bool:
        return True

    def store_chunk(self, artifact_name: str, chunk_id: int, data: str) -> bool:
        if random.random() < FAILURE_PROB:
            return False
        self.store.setdefault(artifact_name, {})[chunk_id] = data

        return True

    def get_chunk(self, artifact_name: str, chunk_id: int) -> (bool, str | None):
        if random.random() < FAILURE_PROB:
            return False, None

        return True, self.store.get(artifact_name, {}).get(chunk_id)

    def delete_artifact(self, artifact_name: str) -> bool:
        self.store.pop(artifact_name, None)

        return True

    def list_chunks(self) -> dict:
        return {self.node_id: self.store.copy()}