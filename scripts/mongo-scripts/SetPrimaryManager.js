/*
 * Copyright 2018 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

const configQuery = { _id: "__GLOBAL_CONFIG_ID__" };
const newValues = {
    $set: {
        "primaryVersion": _version_
    }
};
print("Setting manager primary version: " + _version_);
const res = db.managerConfiguration.findAndModify({ query: configQuery, update: newValues });
const account = db.managerConfiguration.find(configQuery);
print("Manager primary version is");
printjson(account.next().primaryVersion);
