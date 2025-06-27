import json

import ray
from ray.exceptions import RayTaskError
import argparse
import sys

from actors.name_node    import NameNode
from actors.storage_node import StorageNode


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--nodes", type=int, default=8)
    args = parser.parse_args()
    ray.init(runtime_env={"working_dir": "."})

    storage_nodes = [StorageNode.remote(i) for i in range(args.nodes)]
    name_node     = NameNode.remote(storage_nodes)

    help_msg = "Commands: upload <name> <content|@file>, get <name>, update <name> <content|@file>, delete <name>, list, cluster, exit"
    print(help_msg)

    while True:
        cmd = input("> ").strip().split()
        if not cmd:
            continue
        op = cmd[0]

        if op == "upload":
            _, name, *rest = cmd
            raw = rest[0] if rest else ""
            if raw.startswith("@"):
                path = raw[1:]
                try:
                    with open(path, "r", encoding="utf-8") as f:
                        content = f.read()
                except FileNotFoundError:
                    print(f"File not found: {path}")
                    continue
            else:
                content = " ".join(rest)

            try:
                ray.get(name_node.upload.remote(name, content))
                print(f"Uploaded {name}")
            except RayTaskError as rte:
                cause = rte.cause or rte.__cause__ or str(rte)
                print(f"❌ Error uploading '{name}': {cause}")

        elif op == "get":
            _, name = cmd
            try:
                result = ray.get(name_node.get.remote(name))
                if result is None:
                    print(f"Artifact '{name}' not found")
                else:
                    print(result)
            except RayTaskError as rte:
                cause = rte.cause or rte.__cause__ or str(rte)
                print(f"❌ Error getting '{name}': {cause}")

        elif op == "update":
            _, name, *rest = cmd
            content = " ".join(rest)
            try:
                ray.get(name_node.update.remote(name, content))
                print(f"Updated {name}")
            except RayTaskError as rte:
                cause = rte.cause or rte.__cause__ or str(rte)
                print(f"❌ Error updating '{name}': {cause}")

        elif op == "delete":
            _, name = cmd
            try:
                success = ray.get(name_node.delete.remote(name))
                print(f"{'Deleted' if success else 'No such artifact'} {name}")
            except RayTaskError as rte:
                cause = rte.cause or rte.__cause__ or str(rte)
                print(f"❌ Error deleting '{name}': {cause}")

        elif op == "list":
            try:
                status = ray.get(name_node.list_status.remote())
                pretty_print(status)
            except RayTaskError as rte:
                cause = rte.cause or rte.__cause__ or str(rte)
                print(f"❌ Error listing status: {cause}")


        elif op == "exit":
            ray.shutdown()
            sys.exit(0)

        else:
            print(help_msg)

        def pretty_print(data):
            print(json.dumps(data, indent=2))

if __name__ == "__main__":
    main()
