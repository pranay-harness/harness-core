/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

use harness;

function addLearningEngineSecret() {

    if (0 < db.accounts.count()) {
        print("Data found in DB. No need of seeding the learning engine data again.");
        return;
    }

    print("No data found in DB. Seeding learning engine data into it.");

    db.getCollection('serviceSecrets').insert({
        "_id" : "djEzvOJtTFSvpglImf1fXg",
        "serviceSecret" : "{{ .Values.appSecrets.learningEngineSecret }}",
        "serviceType" : "LEARNING_ENGINE",
        "createdAt" : NumberLong(1518718228292),
        "lastUpdatedAt" : NumberLong(1518718228292)
    });
}

addLearningEngineSecret();
