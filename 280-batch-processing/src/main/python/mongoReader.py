import bson
from collections import Counter
from pymongo import MongoClient

MONGO_USERNAME = "<REPLACE-HERE>"
MONGO_PASSWORD = "<REPLACE-HERE>"

client = MongoClient(host=['localhost:61673'], username=MONGO_USERNAME, password=MONGO_PASSWORD, authSource='admin', authMechanism='SCRAM-SHA-1', tls=True, tlsInsecure=True)
db = client.events

query = db.publishedMessages.find({"accountId":"hW63Ny6rQaaGsKkVjE0pJA", "type":"io.harness.perpetualtask.k8s.watch.PodInfo", "attributes.identifier/clusterId":"5ee15b482aa4186d1c9c1ef6", "occurredAt": {"$gt": bson.Int64(1629954205000)}}).sort("occurredAt").limit(10000)

ids = []
for doc in query:
	ids.append(doc['attributes']['identifier/uid'])

counter = Counter(ids)

for x in counter.keys():
	if counter[x] != 2:
		print(x, counter[x])

print(len(ids), len(counter))