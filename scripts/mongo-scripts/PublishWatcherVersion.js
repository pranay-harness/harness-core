/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

const accountQuery = { _id: _accountId_ };
const newValues = {
    $set: {
        "delegateConfiguration.watcherVersion": _version_
    }
};
print("Publishing watcher version: " + _version_ + " for Account: " + _accountId_);
const res = db.accounts.findAndModify({ query: accountQuery, update: newValues });
const account = db.accounts.find(accountQuery);
print("Published watcher version");
printjson(account.next().delegateConfiguration.watcherVersion);
