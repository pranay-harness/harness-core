db.getCollection('workflowExecutions').find({workflowType:"ORCHESTRATION", envIds:{$exists:false}}).forEach(function(we){
	var envIds = [we.envId];
	var serviceIds = [];
		we.serviceExecutionSummaries.forEach(function(summ){
			serviceIds.push(summ.contextElement.uuid);
		});
	db.workflowExecutions.update({ _id: we._id}, { $set: { 'serviceIds': serviceIds, 'envIds': envIds }});
});



db.getCollection('workflowExecutions').find({workflowType:"PIPELINE"}, envIds:{$exists:false}).forEach(function(we){
	var envIds = [];
	var serviceIds = [];
	we.pipelineExecution.pipelineStageExecutions.forEach(function(ps){
		if (ps.workflowExecutions) {
			ps.workflowExecutions.forEach(function(ws){
				envIds.push(ws.envId);
				ws.serviceExecutionSummaries.forEach(function(summ){
					serviceIds.push(summ.contextElement.uuid);
				});
			});
		}
	});

	envIds = Array.from(new Set(envIds));
	serviceIds = Array.from(new Set(serviceIds));
	db.workflowExecutions.update({ _id: we._id}, { $set: { 'serviceIds': serviceIds, 'envIds': envIds }});
});



