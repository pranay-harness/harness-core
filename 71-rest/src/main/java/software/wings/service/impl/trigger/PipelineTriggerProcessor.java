package software.wings.service.impl.trigger;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineCondition;
import software.wings.service.intfc.PipelineService;

@Singleton
@Slf4j
public class PipelineTriggerProcessor implements TriggerProcessor {
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;
  @Inject private transient PipelineService pipelineService;

  @Override
  public void validateTriggerConditionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    PipelineCondition pipelineCondition = (PipelineCondition) deploymentTrigger.getCondition();
    validatePipelineId(deploymentTrigger, pipelineCondition);
  }

  @Override
  public void validateTriggerActionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    triggerServiceHelper.validateTriggerAction(deploymentTrigger);
  }

  @Override
  public void transformTriggerConditionRead(DeploymentTrigger deploymentTrigger) {
    PipelineCondition pipelineCondition = (PipelineCondition) deploymentTrigger.getCondition();

    try {
      String pipelineName =
          pipelineService.fetchPipelineName(deploymentTrigger.getAppId(), pipelineCondition.getPipelineId());
      deploymentTrigger.setCondition(
          PipelineCondition.builder().pipelineId(pipelineCondition.getPipelineId()).pipelineName(pipelineName).build());
    } catch (WingsException exception) {
      throw new WingsException("Pipeline does not exist for pipeline id " + pipelineCondition.getPipelineId(), USER);
    }
  }

  @Override
  public void transformTriggerActionRead(DeploymentTrigger deploymentTrigger) {
    triggerServiceHelper.reBuildTriggerActionWithNames(deploymentTrigger);
  }

  @Override
  public void executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams) {}

  private void validatePipelineId(DeploymentTrigger deploymentTrigger, PipelineCondition pipelineCondition) {
    try {
      pipelineService.fetchPipelineName(deploymentTrigger.getAppId(), pipelineCondition.getPipelineId());
    } catch (WingsException exception) {
      throw new WingsException("Pipeline does not exist for pipeline id " + pipelineCondition.getPipelineId(), USER);
    }
  }
}
