//mongeez formatted javascript
//changeset peeyushaggarwal:account-migration

if(db.account.count() == 0) {
  db.account.insert({ "_id" : "kmpySmUISimoRrJL6NL73w", "companyName" : "Wings Software",
  "appId" : "__GLOBAL_APP_ID__", "createdAt" : 1476312420147, "lastUpdatedAt" : 1476312421284 });
  db.applications.updateMany({}, { "accountId": "kmpySmUISimoRrJL6NL73w"});
  db.settingAttributes.updateMany({}, { "accountId": "kmpySmUISimoRrJL6NL73w"});
  db.notifications.updateMany({}, { "accountId": "kmpySmUISimoRrJL6NL73w"});
  db.roles.updateMany({}, { "accountId": "kmpySmUISimoRrJL6NL73w"});
  db.users.updateMany({}, { "accounts": ["kmpySmUISimoRrJL6NL73w"]});
  db.appContainers.updateMany({}, { "accountId": "kmpySmUISimoRrJL6NL73w"});
}
