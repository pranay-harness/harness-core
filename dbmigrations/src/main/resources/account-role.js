//mongeez formatted javascript
//changeset rishi:account-role

db.accounts.find().forEach(function(account){
db.accounts.update({_id:account._id}, {$set: { "accountName" : account.companyName}}
)});

db.accounts.dropIndex("companyName_1");

