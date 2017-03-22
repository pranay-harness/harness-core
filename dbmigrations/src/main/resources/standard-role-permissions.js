//mongeez formatted javascript
//changeset rishi:standard-role-permissions

db.roles.update({}, {$unset: {permissions:1}}, {multi:true});

