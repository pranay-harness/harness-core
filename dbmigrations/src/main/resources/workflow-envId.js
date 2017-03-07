//mongeez formatted javascript
//changeset rishi:workflow-envId

db.orchWorkflows.find().forEach(function(elem){
 db.orchWorkflows.update(
            {
                _id: elem._id
            },
            {
                $set: {
                    envId: elem.environmentId
                }
            }
        );
});