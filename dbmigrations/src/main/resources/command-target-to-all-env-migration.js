//mongeez formatted javascript
//changeset peeyushaggarwal:command-target-to-all-env-migration

db.serviceCommands.updateMany({}, { $set: { targetToAllEnv: true }});

