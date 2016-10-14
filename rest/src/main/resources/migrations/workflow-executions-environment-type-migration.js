//mongeez formatted javascript
//changeset anubhawsrivastava:workflow-executions-environment-type-migration

var envIds = [];
db.environments.find().forEach(function(env){
    envIds.push(env._id);
    db.workflowExecutions.updateMany({envId:env._id}, {$set:{envType:env.environmentType}});
});

db.workflowExecutions.updateMany({envId: {$nin:envIds}}, {$set:{envType:"OTHER"}});
