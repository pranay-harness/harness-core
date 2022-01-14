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