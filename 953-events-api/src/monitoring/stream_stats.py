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

def get_behind_count(redis_key, start_id, end_id):
  count = 0
  batch_size = 10000
  while True:
    ids = client.xrange(redis_key, start_id, end_id, batch_size)
    retrieved_size = len(ids)
    # Adding "- 1" because the start_id and end_id is inclusive and we want to have start_id excluded
    count += retrieved_size - 1

    if retrieved_size <= 1:
          break
    start_id = ids[-1][0]

  return count

for key in client.scan_iter("*streams:*"):
  redis_key = key.decode("utf-8")
  usecase_name = redis_key.replace("streams:", "")
  if redis_key.startswith("streams:deadletter_queue:"):
    continue

  group_infos = client.xinfo_groups(key)
  stream_info = client.xinfo_stream(key)
  last_created_id = stream_info["last-generated-id"].decode("utf-8")
  group_info_table = PrettyTable(["ConsumerGroup", "Consumer count", "Pending count",
                                  "Last Read", "Unread messages"])

  print("\033[1m\033[93m" + redis_key + "\033[0;0m")
  print("Dead letter queue size:", client.xlen("streams:deadletter_queue:" + usecase_name))
  print("Stream length:", client.xlen(redis_key))

  for group_info in group_infos:
    last_read_by_group_id = group_info["last-delivered-id"].decode("utf-8")
    group_info_table.add_row([
      group_info["name"].decode("utf-8"),
      group_info["consumers"],
      group_info["pending"],
      last_read_by_group_id,
      get_behind_count(redis_key, last_read_by_group_id, last_created_id)
    ])

  print(group_info_table)
  print("\n")
