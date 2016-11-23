//mongeez formatted javascript
//changeset peeyushaggarwal:config-version-migration

function guid() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}

function guid_to_base64(g, le) {
  var hexlist = '0123456789abcdef';
  var b64list = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
  var s = g.replace(/[^0-9a-f]/ig, '').toLowerCase();
  if (s.length != 32) return '';

  if (le) s = s.slice(6, 8) + s.slice(4, 6) + s.slice(2, 4) + s.slice(0, 2) +
    s.slice(10, 12) + s.slice(8, 10) +
    s.slice(14, 16) + s.slice(12, 14) +
    s.slice(16);
  s += '0';

  var a, p, q;
  var r = '';
  var i = 0;
  while (i < 33) {
    a = (hexlist.indexOf(s.charAt(i++)) << 8) |
      (hexlist.indexOf(s.charAt(i++)) << 4) |
      (hexlist.indexOf(s.charAt(i++)));

    p = a >> 6;
    q = a & 63;

    r += b64list.charAt(p) + b64list.charAt(q);
  }

  return r;
}

db.applications.find().forEach(function(app){
 db.configFiles.find({ appId: app._id}).forEach(function(configFile) {
   if(!configFile.defaultVersion) {
     db.configFiles.update({ _id: configFile._id }, { $set: { defaultVersion: 1, targetToAllEnv: true}});
     db.configs.files.update({ _id: ObjectId(configFile.fileUuid) }, { $set: { "metadata.version": 1 }});
     db.entityVersions.insert({ "_id" : guid_to_base64(guid(), false), "entityType" : "CONFIG", "entityName" : configFile.fileName, "entityParentUuid": configFile.entityId,"changeType" : "CREATED", "entityUuid" : configFile._id, "version" : 1, "appId" : configFile.appId,  "createdAt" : NumberLong("1478646591138"),  "lastUpdatedAt" : NumberLong("1478646591138") });
   }
 });
});

