const accountQuery = { _id: _accountId_ };
const newValues = {
    $addToSet: {
        "delegateConfiguration.delegateVersions": _version_
    }
};
print("Publishing delegate version: " + _version_ + " for Account: " + _accountId_);
const res = db.accounts.findAndModify({ query: accountQuery, update: newValues });
const account = db.accounts.find(accountQuery);
print("Published delegate Versions");
printjson(account.next().delegateConfiguration.delegateVersions);