package software.wings.service.impl.trigger;

import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.Condition.Type;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class DeploymentTriggerServiceImpl implements DeploymentTriggerService {
  @Inject private Map<String, TriggerProcessor> triggerProcessorMapBinder;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowService workflowService;

  @Override
  public DeploymentTrigger save(DeploymentTrigger trigger) {
    setWorkflowName(trigger);

    TriggerProcessor triggerProcessor = obtainTriggerProcessor(trigger);

    triggerProcessor.validateTriggerCondition(trigger);

    return duplicateCheck(
        () -> wingsPersistence.saveAndGet(DeploymentTrigger.class, trigger), "name", trigger.getName());
  }

  @Override
  public DeploymentTrigger update(DeploymentTrigger trigger) {
    DeploymentTrigger existingTrigger =
        wingsPersistence.getWithAppId(DeploymentTrigger.class, trigger.getAppId(), trigger.getUuid());
    notNullCheck("Trigger was deleted ", existingTrigger, USER);
    equalCheck(trigger.getWorkflowType(), existingTrigger.getWorkflowType());

    setWorkflowName(trigger);

    TriggerProcessor triggerProcessor = obtainTriggerProcessor(trigger);

    triggerProcessor.validateTriggerCondition(trigger);

    return duplicateCheck(
        () -> wingsPersistence.saveAndGet(DeploymentTrigger.class, trigger), "name", trigger.getName());
  }

  @Override
  public void delete(String appId, String triggerId) {
    wingsPersistence.delete(DeploymentTrigger.class, triggerId);
  }

  @Override
  public DeploymentTrigger get(String appId, String triggerId) {
    return wingsPersistence.getWithAppId(DeploymentTrigger.class, appId, triggerId);
  }

  @Override
  public PageResponse<DeploymentTrigger> list(PageRequest<DeploymentTrigger> pageRequest) {
    return wingsPersistence.query(DeploymentTrigger.class, pageRequest);
  }

  private TriggerProcessor obtainTriggerProcessor(DeploymentTrigger deploymentTrigger) {
    return triggerProcessorMapBinder.get(obtainTriggerConditionType(deploymentTrigger.getCondition()));
  }

  private String obtainTriggerConditionType(Condition condition) {
    if (condition instanceof ArtifactCondition) {
      return Type.NEW_ARTIFACT.name();
    }
    throw new InvalidRequestException("Invalid Trigger Condition", USER);
  }

  public void setWorkflowName(DeploymentTrigger trigger) {
    switch (trigger.getWorkflowType()) {
      case PIPELINE:
        trigger.setWorkflowName(pipelineService.fetchPipelineName(trigger.getAppId(), trigger.getWorkflowId()));
        break;
      case ORCHESTRATION:
        trigger.setWorkflowName(workflowService.fetchWorkflowName(trigger.getAppId(), trigger.getWorkflowId()));
        break;
      default:
        unhandled(trigger.getWorkflowType());
    }
  }
}