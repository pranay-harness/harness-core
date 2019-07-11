package software.wings.service.impl.trigger;

import static software.wings.beans.trigger.ArtifactCondition.ArtifactConditionBuilder;
import static software.wings.beans.trigger.DeploymentTrigger.DeploymentTriggerBuilder;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.inject.Singleton;

import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.WorkflowAction;

@Singleton
public class DeploymentTriggerGenerator {
  public DeploymentTrigger ensureDeploymentTrigger(DeploymentTrigger trigger) {
    final DeploymentTriggerBuilder triggerBuilder = DeploymentTrigger.builder();

    if (trigger.getAppId() != null) {
      triggerBuilder.appId(trigger.getAppId());
    } else {
      triggerBuilder.appId(APP_ID);
    }

    if (trigger.getAction() instanceof WorkflowAction) {
      WorkflowAction workflowAction = (WorkflowAction) trigger.getAction();

      if (workflowAction != null) {
        triggerBuilder.action(workflowAction);
      } else {
        triggerBuilder.action(WorkflowAction.builder().workflowId(WORKFLOW_ID).build());
      }
    } else if (trigger.getAction() instanceof PipelineAction) {
      PipelineAction pipelineAction = (PipelineAction) trigger.getAction();
      if (pipelineAction != null) {
        triggerBuilder.action(pipelineAction);
      } else {
        triggerBuilder.action(PipelineAction.builder().pipelineId(PIPELINE_ID).build());
      }
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
      } else {
        artifactConditionBuilder.artifactStreamId(artifactCondition.getArtifactStreamId());
      }
      triggerBuilder.condition(artifactConditionBuilder.build());
    }
    return triggerBuilder.build();
  }
}
