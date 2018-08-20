const managerConfigQuery = { _id: "__GLOBAL_CONFIG_ID__" };
const managerConfig = db.managerConfiguration.find(managerConfigQuery);
const primaryVersion = managerConfig.next().primaryVersion;
print("Manager primary version is");
printjson(primaryVersion);

const delegateVersionSet = primaryVersion === '*' ?  new Set([_version_]) : new Set([primaryVersion, _version_]);
const delegateVersions = Array.from(delegateVersionSet)

const accountQuery = { _id: _accountId_ };
const newValues = {
    $set: {
        "delegateConfiguration.delegateVersions": delegateVersions
    }
};
print("Publishing new delegate version: " + _version_ + " for Account: " + _accountId_);
const res = db.accounts.findAndModify({ query: accountQuery, update: newValues });
const account = db.accounts.find(accountQuery);
print("Published delegate Versions");
printjson(account.next().delegateConfiguration.delegateVersions);