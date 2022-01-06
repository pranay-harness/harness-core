/*
 * Copyright 2018 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
