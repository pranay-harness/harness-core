package software.wings.service.impl.trigger;

import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.DeploymentTrigger;

public interface TriggerProcessor {
  void validateTriggerConditionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger);

  void validateTriggerActionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger);

  void transformTriggerConditionRead(DeploymentTrigger deploymentTrigger);

  void transformTriggerActionRead(DeploymentTrigger deploymentTrigger, boolean readPrimaryVariablesValueNames);

  WorkflowExecution executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams);
}
