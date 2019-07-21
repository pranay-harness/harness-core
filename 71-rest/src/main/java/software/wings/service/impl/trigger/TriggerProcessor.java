package software.wings.service.impl.trigger;

import software.wings.beans.trigger.DeploymentTrigger;

public interface TriggerProcessor {
  void validateTriggerConditionSetup(DeploymentTrigger deploymentTrigger);

  void validateTriggerActionSetup(DeploymentTrigger deploymentTrigger);

  void transformTriggerConditionRead(DeploymentTrigger deploymentTrigger);

  void transformTriggerActionRead(DeploymentTrigger deploymentTrigger);

  void executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams);
}
