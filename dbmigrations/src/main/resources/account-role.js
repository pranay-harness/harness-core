//mongeez formatted javascript
//changeset rishi:account-role

db.accounts.find().forEach(function(account){
db.accounts.update({_id:account._id}, {$set: { "accountName" : account.companyName}}
)});


if(db.accounts.getIndexes().map(function(index) {  return index.name; }).indexOf("companyName_1") > -1) {
  db.accounts.dropIndex('companyName_1');
}

