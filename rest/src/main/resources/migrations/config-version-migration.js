//mongeez formatted javascript
//changeset peeyushaggarwal:config-version-migration

db.applications.find().forEach(function(app){
 db.configFiles.find({ appId: app._id}).forEach(function(configFile) {
   if(!configFile.defaultVersion) {
     db.configFiles.update({ _id: configFile._id }, { $set: { defaultVersion: 1, targetToAllEnv: true}});
     db.configs.files.update({ _id: ObjectId(configFile.fileUuid) }, { $set: { "metadata.version": 1 }});
   }
 });
});

