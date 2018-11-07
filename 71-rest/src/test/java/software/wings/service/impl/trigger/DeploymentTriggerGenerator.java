package software.wings.service.impl.trigger;

import static software.wings.beans.trigger.ArtifactCondition.ArtifactConditionBuilder;
import static software.wings.beans.trigger.DeploymentTrigger.DeploymentTriggerBuilder;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.inject.Singleton;

import software.wings.beans.WorkflowType;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.DeploymentTrigger;

@Singleton
public class DeploymentTriggerGenerator {
  public DeploymentTrigger ensureDeploymentTrigger(DeploymentTrigger trigger) {
    final DeploymentTriggerBuilder triggerBuilder = DeploymentTrigger.builder();

    if (trigger.getAppId() != null) {
      triggerBuilder.appId(trigger.getAppId());
    } else {
      triggerBuilder.appId(APP_ID);
    }

    if (trigger.getWorkflowType() != null) {
      triggerBuilder.workflowType(trigger.getWorkflowType());
    } else {
      triggerBuilder.workflowType(WorkflowType.PIPELINE);
    }

    if (trigger.getWorkflowId() != null) {
      triggerBuilder.workflowId(trigger.getWorkflowId());
    } else {
      triggerBuilder.workflowId(WORKFLOW_ID);
    }
    if (trigger.getName() != null) {
      triggerBuilder.name(trigger.getName());
    } else {
      triggerBuilder.name("Trigger Test");
    }
    Condition triggerCondition = trigger.getCondition();
    if (triggerCondition instanceof ArtifactCondition) {
      ArtifactCondition artifactCondition = (ArtifactCondition) triggerCondition;
      final ArtifactConditionBuilder artifactConditionBuilder = ArtifactCondition.builder();
      if (artifactCondition.getArtifactStreamId() == null) {
        artifactConditionBuilder.artifactStreamId(ARTIFACT_STREAM_ID);
      }
      triggerBuilder.condition(artifactConditionBuilder.build());
    }
    return triggerBuilder.build();
  }
}
