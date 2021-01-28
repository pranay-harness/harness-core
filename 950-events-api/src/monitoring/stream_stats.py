import redis
import os
import sys
from prettytable import PrettyTable

try:
  redis_connection = os.environ["redis_connection"]
except KeyError:
  print("Please set environment variable `redis_connection` (Example: redis_connection=localhost:6379)")
  sys.exit(1)

redis_password = os.environ.get("redis_password", "")

redis_host, redis_port = redis_connection.split(":")

client = redis.Redis(host=redis_host, port=redis_port, password=redis_password)

for key in client.scan_iter("streams:*"):
  redis_key = key.decode("utf-8")
  usecase_name = redis_key.replace("streams:", "")
  if redis_key.startswith("streams:deadletter_queue:"):
    continue

  group_infos = client.xinfo_groups(key)
  print("\u001B[33m" + redis_key + "\u001B[0m")
  print("Dead letter queue size:", client.xlen("streams:deadletter_queue:" + usecase_name))
  group_info_table = PrettyTable(["ConsumerGroup", "Consumer count", "Pending count", "Last Delivered"])
  for group_info in group_infos:
    group_info_table.add_row([
      group_info["name"].decode("utf-8"),
      group_info["consumers"],
      group_info["pending"],
      group_info["last-delivered-id"].decode("utf-8")
    ])

  print(group_info_table)
  print("\n")
