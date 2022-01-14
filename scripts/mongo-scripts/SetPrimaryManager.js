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