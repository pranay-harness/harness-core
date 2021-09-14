/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
