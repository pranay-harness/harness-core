/*
 * Copyright 2018 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

const managerConfigQuery = { _id: "__GLOBAL_CONFIG_ID__" };
const managerConfig = db.managerConfiguration.find(managerConfigQuery);
const primaryVersion = managerConfig.next().primaryVersion;
print("Manager primary version is");
printjson(primaryVersion);

const accountQuery = { _id: _accountId_ };
const newValues = {
    $set: {
        "delegateConfiguration.delegateVersions": [primaryVersion]
    }
};
const res = db.accounts.findAndModify({ query: accountQuery, update: newValues });
const account = db.accounts.find(accountQuery);
print("Published Delegate Versions");
printjson(account.next().delegateConfiguration.delegateVersions);
