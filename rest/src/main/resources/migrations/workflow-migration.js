//mongeez formatted javascript
//changeset peeyushaggarwal:workflow-migration

db.applications.find().forEach(function(app){
 if(db.orchestrations.find({ appId: app._id, name: "Canary Deployment"}).count() > 1) {
   var canaryDeploymentIds = [];
   db.orchestrations.find({ appId: app._id, name: "Canary Deployment"}).forEach(function(wf) { canaryDeploymentIds.push(wf._id);});
   db.stateMachines.updateMany({appId: app._id, originId: { "$in": canaryDeploymentIds }}, { "$set": { originId: canaryDeploymentIds[0]}});
   db.orchestrations.remove({ appId: app._id, _id: canaryDeploymentIds[1]});
   db.orchestrations.remove({ appId: app._id, _id: canaryDeploymentIds[2]});
   db.orchestrations.remove({ appId: app._id, _id: canaryDeploymentIds[3]});
   db.orchestrations.updateMany({ appId: app._id }, { "$set": { targetToAllEnv: true}, "$unset": { envId: ""} });
 }
});

