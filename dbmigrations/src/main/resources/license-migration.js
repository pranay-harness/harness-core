//mongeez formatted javascript
//changeset peeyushaggarwal:license-migration

db.licenses.insert({ "_id" : "E98a9vSlRQ6vK8QiSQ-vpQ", "name" : "Trial", "isActive" : true, "expiryDuration" : NumberLong("31536000000"),
 "createdAt" : NumberLong("1490303435070"), "lastUpdatedAt" : NumberLong("1490303435070") });
db.accounts.updateMany({}, { $set: { "licenseId" : "E98a9vSlRQ6vK8QiSQ-vpQ",  "licenseExpiryTime" : NumberLong("1521839435709")}});
