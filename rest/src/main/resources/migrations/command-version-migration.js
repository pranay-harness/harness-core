//mongeez formatted javascript
//changeset peeyushaggarwal:command-version-migration

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
  db.services.find({ appId: app._id}).forEach(function(service){
    if(service.commands) {
      var serviceCommandIds = [];
      var commandNameMap = {};
      service.commands.forEach(function(command) {
        var serviceCommandId = guid_to_base64(guid(), false);
        db.serviceCommands.insert({ _id: serviceCommandId, name: command.name, appId: app._id, defaultVersion: 1, serviceId: service._id});
        command["appId"] = app._id;
        command["version"] = NumberLong(1);
        command["originEntityId"] = serviceCommandId;
        command["_id"] = guid_to_base64(guid(), false);
        db.entityVersions.insert({ "_id" : guid_to_base64(guid(), false), "entityType" : "COMMAND", "entityName" : command.name, "entityParentUuid": service._id,"changeType" : "CREATED", "entityUuid" : serviceCommandId, "version" : 1, "appId" : app._id,  "createdAt" : NumberLong("1478646591138"),  "lastUpdatedAt" : NumberLong("1478646591138") });
        serviceCommandIds.push(serviceCommandId);
        commandNameMap[command.name] = 1;
        db.commands.insert(command);
      });
      db.services.update({ _id: service._id}, { $set: { serviceCommands: serviceCommandIds }, $unset: { commands: "", oldCommands: "" } });
      db.activities.updateMany({ serviceId: service._id, type: "Command"}, { $set : { commandNameVersionMap: commandNameMap } });
    }
  });
});

