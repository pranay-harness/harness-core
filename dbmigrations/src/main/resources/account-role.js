

db.accounts.find().forEach(function(account){
db.accounts.update({_id:account._id}, {$set: { "accountName" : account.companyName}}
)});