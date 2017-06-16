//mongeez formatted javascript
//changeset peeyushaggarwal:account-migration

if(db.accounts.count() == 0) {
  db.accounts.insert({ "_id" : "kmpySmUISimoRrJL6NL73w", "companyName" : "Harness Inc",
  "appId" : "__GLOBAL_APP_ID__", "createdAt" : NumberLong("1476312420147"), "lastUpdatedAt" : NumberLong("1476312421284") });
  db.applications.updateMany({}, { "$set": { "accountId": "kmpySmUISimoRrJL6NL73w"}});
  db.settingAttributes.updateMany({}, { "$set": { "accountId": "kmpySmUISimoRrJL6NL73w"}});
  db.notifications.updateMany({}, { "$set": { "accountId": "kmpySmUISimoRrJL6NL73w"}});
  db.roles.updateMany({}, { "$set": { "accountId": "kmpySmUISimoRrJL6NL73w"}});
  db.users.updateMany({}, { "$set": { "accounts": [ "kmpySmUISimoRrJL6NL73w"]}});
  db.appContainers.updateMany({}, { "$set": { "accountId": "kmpySmUISimoRrJL6NL73w"}});
  db.settingAttributes.updateMany({ name: { "$in": ["Wings Jenkins", "SMTP", "Splunk", "AppDynamics"] }}, { "$set": { "isPluginSetting": true }});
}
