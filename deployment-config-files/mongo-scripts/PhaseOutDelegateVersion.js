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