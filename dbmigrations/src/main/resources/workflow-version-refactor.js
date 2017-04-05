//mongeez formatted javascript
//changeset rishi:workflow-version-refactor.js

db.activities.updateMany({workflowType: "ORCHESTRATION_WORKFLOW"}, {$set:{workflowType: "ORCHESTRATION"}});
db.workflowExecutions.updateMany({workflowType: "ORCHESTRATION_WORKFLOW"}, {$set:{workflowType: "ORCHESTRATION"}});
