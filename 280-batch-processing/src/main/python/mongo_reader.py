#!/usr/bin/env python3

# The purpose of this file is to connect to any mongo server (Prod, Prod-Free, QA, UAT, ce-dev, etc) and execute any query on it.
# This can be used to run script on mongo collection and debug any issue.

import sys
import bson
from pymongo import MongoClient

# This is the address of the ssh tunnel created to connect to mongo server. 
# Ref: https://github.com/wings-software/portal/pull/26974#issue-722184050

assert (
    len(sys.argv) == 4
), "Use it as: ./mongo_reader.py username password localhost:60166"

MONGO_USERNAME = sys.argv[1]
MONGO_PASSWORD = sys.argv[2]
HOST = sys.argv[3]  # "localhost:61673"

# Do all of your processing on the db here.
def main(db):
    # Example, feel free to rewrite it to debug any issue.
    from collections import Counter

    query = {
        "accountId": "hW63Ny6rQaaGsKkVjE0pJA",
        "type": "io.harness.perpetualtask.k8s.watch.PodInfo",
        "attributes.identifier/clusterId": "5ee15b482aa4186d1c9c1ef6",
        "occurredAt": {"$gt": bson.Int64(1629954205000)},
    }
    cursor = db.publishedMessages.find(query).sort("occurredAt").limit(10)

    ids = []
    for doc in cursor:
        ids.append(doc["attributes"]["identifier/uid"])

    counter = Counter(ids)
    print(len(ids), len(counter))


if __name__ == "__main__":
    client = MongoClient(
        host=[HOST],
        username=MONGO_USERNAME,
        password=MONGO_PASSWORD,
        authSource="admin",
        authMechanism="SCRAM-SHA-1",
        tls=True,
        tlsInsecure=True,
    )

    # print(client.server_info())

    db = client.events  # Replace the name of the db you want to use.
    main(db)
