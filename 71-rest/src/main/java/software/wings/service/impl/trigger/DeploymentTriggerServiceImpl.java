package software.wings.service.impl.trigger;

import static io.harness.exception.WingsException.USER;
import static software.wings.beans.trigger.Condition.Type.NEW_ARTIFACT;
import static software.wings.beans.trigger.Condition.Type.PIPELINE_COMPLETION;
import static software.wings.beans.trigger.Condition.Type.SCHEDULED;
import static software.wings.beans.trigger.Condition.Type.WEBHOOK;
import static software.wings.scheduler.ScheduledTriggerJob.PREFIX;
import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Event;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.trigger.PipelineTriggerProcessor.PipelineTriggerExecutionParams;
import software.wings.service.impl.trigger.ScheduleTriggerProcessor.ScheduledTriggerExecutionParams;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class DeploymentTriggerServiceImpl implements DeploymentTriggerService {
  @Inject private transient Map<String, TriggerProcessor> triggerProcessorMapBinder;
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient PipelineService pipelineService;
  @Inject private transient WorkflowService workflowService;
  @Inject private transient YamlPushService yamlPushService;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;
  @Inject private transient AppService appService;

  @Override
  public DeploymentTrigger save(DeploymentTrigger trigger) {
    String accountId = appService.getAccountIdByAppId(trigger.getAppId());
    trigger.setAccountId(accountId);
    validateTrigger(trigger, null);
    setConditionTypeInTrigger(trigger);
    String uuid = Validator.duplicateCheck(() -> wingsPersistence.save(trigger), "name", trigger.getName());
    return get(trigger.getAppId(), uuid);
    // Todo Uncomment once YAML support is added  actionsAfterTriggerSave(deploymentTrigger);
  }

  @Override
  public DeploymentTrigger update(DeploymentTrigger trigger) {
    String accountId = appService.getAccountIdByAppId(trigger.getAppId());
    trigger.setAccountId(accountId);

    DeploymentTrigger existingTrigger =
        wingsPersistence.getWithAppId(DeploymentTrigger.class, trigger.getAppId(), trigger.getUuid());
    notNullCheck("Trigger was deleted ", existingTrigger, USER);
    equalCheck(trigger.getAction().getActionType(), existingTrigger.getAction().getActionType());

    validateTrigger(trigger, existingTrigger);
    setConditionTypeInTrigger(trigger);
    String uuid = Validator.duplicateCheck(() -> wingsPersistence.save(trigger), "name", trigger.getName());
    return get(trigger.getAppId(), uuid);
    // Todo Uncomment once YAML support is added actionsAfterTriggerUpdate(existingTrigger, deploymentTrigger);
  }

  @Override
  public void delete(String appId, String triggerId) {
    DeploymentTrigger deploymentTrigger = get(appId, triggerId);
    notNullCheck("Trigger not exist ", triggerId, USER);
    // Todo Uncomment once YAML support is added  actionsAfterTriggerDelete(deploymentTrigger);
    // Do we have to prune tag links ?
    wingsPersistence.delete(DeploymentTrigger.class, triggerId);
  }

  @Override
  public DeploymentTrigger get(String appId, String triggerId) {
    DeploymentTrigger deploymentTrigger = wingsPersistence.getWithAppId(DeploymentTrigger.class, appId, triggerId);
    notNullCheck("Trigger not exist ", triggerId, USER);
    TriggerProcessor triggerProcessor = obtainTriggerProcessor(deploymentTrigger);
    triggerProcessor.transformTriggerConditionRead(deploymentTrigger);
    triggerProcessor.transformTriggerActionRead(deploymentTrigger);
    return deploymentTrigger;
  }

  @Override
  public PageResponse<DeploymentTrigger> list(PageRequest<DeploymentTrigger> pageRequest) {
    PageResponse<DeploymentTrigger> response = wingsPersistence.query(DeploymentTrigger.class, pageRequest);

    response.getResponse().forEach(deploymentTrigger -> {
      TriggerProcessor triggerProcessor = obtainTriggerProcessor(deploymentTrigger);
      triggerProcessor.transformTriggerConditionRead(deploymentTrigger);
      triggerProcessor.transformTriggerActionRead(deploymentTrigger);
    });
    return response;
  }

  @Override
  public void triggerExecutionPostArtifactCollectionAsync(
      String accountId, String appId, String artifactStreamId, List<Artifact> artifacts) {
    ArtifactTriggerProcessor triggerProcessor =
        (ArtifactTriggerProcessor) triggerProcessorMapBinder.get(NEW_ARTIFACT.name());

    triggerProcessor.executeTriggerOnEvent(appId,
        ArtifactTriggerProcessor.ArtifactTriggerExecutionParams.builder()
            .artifactStreamId(artifactStreamId)
            .collectedArtifacts(artifacts)
            .build());
  }

  @Override
  public String getCronDescription(String expression) {
    return triggerServiceHelper.getCronDescription(PREFIX + expression);
  }

  @Override
  public void triggerScheduledExecutionAsync(DeploymentTrigger trigger) {
    ScheduleTriggerProcessor triggerProcessor =
        (ScheduleTriggerProcessor) triggerProcessorMapBinder.get(SCHEDULED.name());

    triggerProcessor.executeTriggerOnEvent(
        trigger.getAppId(), ScheduledTriggerExecutionParams.builder().trigger(trigger).build());
  }

  @Override
  public void triggerExecutionPostPipelineCompletionAsync(String appId, String pipelineId) {
    PipelineTriggerProcessor triggerProcessor =
        (PipelineTriggerProcessor) triggerProcessorMapBinder.get(PIPELINE_COMPLETION.name());

    triggerProcessor.executeTriggerOnEvent(
        appId, PipelineTriggerExecutionParams.builder().pipelineId(pipelineId).build());
  }

  private void validateTrigger(DeploymentTrigger trigger, DeploymentTrigger existingTrigger) {
    TriggerProcessor triggerProcessor = obtainTriggerProcessor(trigger);

    triggerProcessor.validateTriggerConditionSetup(trigger, existingTrigger);
    triggerProcessor.validateTriggerActionSetup(trigger, existingTrigger);
  }

  private void setConditionTypeInTrigger(DeploymentTrigger trigger) {
    trigger.setType(trigger.getCondition().getType());
  }

  private TriggerProcessor obtainTriggerProcessor(DeploymentTrigger deploymentTrigger) {
    return triggerProcessorMapBinder.get(obtainTriggerConditionType(deploymentTrigger.getCondition()));
  }

  private String obtainTriggerConditionType(Condition condition) {
    if (condition.getType().equals(NEW_ARTIFACT) || condition.getType().equals(SCHEDULED)
        || condition.getType().equals(PIPELINE_COMPLETION) || condition.getType().equals(WEBHOOK)) {
      return condition.getType().name();
    }
    throw new InvalidRequestException("Invalid Trigger Condition for trigger " + condition.getType().name(), USER);
  }

  void actionsAfterTriggerRead(DeploymentTrigger existingTrigger, DeploymentTrigger updatedTrigger) {
    String accountId = appService.getAccountIdByAppId(updatedTrigger.getAppId());

    boolean isRename = !existingTrigger.getName().equals(updatedTrigger.getName());
    yamlPushService.pushYamlChangeSet(accountId, existingTrigger, updatedTrigger, Event.Type.UPDATE, false, isRename);
  }

  void actionsAfterTriggerDelete(DeploymentTrigger savedTrigger) {
    String accountId = appService.getAccountIdByAppId(savedTrigger.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, null, savedTrigger, Event.Type.DELETE, false, false);
  }

  void actionsAfterTriggerSave(DeploymentTrigger savedTrigger) {
    String accountId = appService.getAccountIdByAppId(savedTrigger.getAppId());

    yamlPushService.pushYamlChangeSet(accountId, null, savedTrigger, Event.Type.CREATE, false, false);
  }

  void actionsAfterTriggerUpdate(DeploymentTrigger existingTrigger, DeploymentTrigger updatedTrigger) {
    String accountId = appService.getAccountIdByAppId(updatedTrigger.getAppId());

    boolean isRename = !existingTrigger.getName().equals(updatedTrigger.getName());
    yamlPushService.pushYamlChangeSet(accountId, existingTrigger, updatedTrigger, Event.Type.UPDATE, false, isRename);
  }
}
