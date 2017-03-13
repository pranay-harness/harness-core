//mongeez formatted javascript
//changeset rishi:role-type-migration


db.roles.update(
{},
{
$set: { "roleType" : "ACCOUNT_ADMIN" }
}
);

db.roles.update(
{"permissions":{$elemMatch: {"environmentType":"QA"}}},
{
$set: { "permissions.$.environmentType" : "NON_PROD" }
}
);

db.roles.update(
{"permissions":{$elemMatch: {"environmentType":"DEV"}}},
{
$set: { "permissions.$.environmentType" : "NON_PROD" }
}
);
