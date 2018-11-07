package software.wings.service.impl.trigger;

import software.wings.beans.trigger.DeploymentTrigger;

public interface TriggerProcessor { void validateTriggerCondition(DeploymentTrigger deploymentTrigger); }
