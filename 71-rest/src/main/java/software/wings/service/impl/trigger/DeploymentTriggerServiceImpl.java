package software.wings.service.impl.trigger;

import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.util.stream.Collectors.toList;
import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.beans.Event;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.Condition.Type;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactSelectionArtifact;
import software.wings.beans.trigger.TriggerArtifactSelectionPipeline;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.trigger.TriggerArtifactSelectionWorkflow;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
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
  @Inject private transient EnvironmentService environmentService;
  @Inject private transient ServiceResourceService serviceResourceService;

  @Override
  public DeploymentTrigger save(DeploymentTrigger trigger) {
    setWorkflowName(trigger);
    validateTrigger(trigger);

    String uuid = Validator.duplicateCheck(() -> wingsPersistence.save(trigger), "name", trigger.getName());
    return get(trigger.getAppId(), uuid);
    // Todo Uncomment once YAML support is added  actionsAfterTriggerSave(deploymentTrigger);
  }

  @Override
  public DeploymentTrigger update(DeploymentTrigger trigger) {
    DeploymentTrigger existingTrigger =
        wingsPersistence.getWithAppId(DeploymentTrigger.class, trigger.getAppId(), trigger.getUuid());
    notNullCheck("Trigger was deleted ", existingTrigger, USER);
    equalCheck(trigger.getAction().getActionType(), existingTrigger.getAction().getActionType());

    setWorkflowName(trigger);
    validateTrigger(trigger);
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
    triggerProcessor.updateTriggerCondition(deploymentTrigger);
    setTriggerAction(deploymentTrigger);
    return deploymentTrigger;
  }

  @Override
  public PageResponse<DeploymentTrigger> list(PageRequest<DeploymentTrigger> pageRequest) {
    return wingsPersistence.query(DeploymentTrigger.class, pageRequest);
  }

  @Override
  public void triggerExecutionPostArtifactCollectionAsync(
      String accountId, String appId, String artifactStreamId, List<Artifact> artifacts) {
    ArtifactTriggerProcessor triggerProcessor =
        (ArtifactTriggerProcessor) triggerProcessorMapBinder.get(Type.NEW_ARTIFACT.name());

    triggerProcessor.triggerExecutionPostArtifactCollection(appId, artifactStreamId, artifacts);
  }

  private void setTriggerAction(DeploymentTrigger deploymentTrigger) {
    switch (deploymentTrigger.getAction().getActionType()) {
      case PIPELINE:
        PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
        TriggerArgs triggerArgs = pipelineAction.getTriggerArgs();
        List<TriggerArtifactVariable> triggerArtifactVariables =
            setTriggerArtifactVariables(deploymentTrigger.getAppId(), triggerArgs.getTriggerArtifactVariables());

        deploymentTrigger.setAction(
            PipelineAction.builder()
                .pipelineId(pipelineAction.getPipelineId())
                .pipelineName(
                    pipelineService.fetchPipelineName(deploymentTrigger.getAppId(), pipelineAction.getPipelineId()))
                .triggerArgs(TriggerArgs.builder()
                                 .excludeHostsWithSameArtifact(triggerArgs.isExcludeHostsWithSameArtifact())
                                 .variables(triggerArgs.getVariables())
                                 .triggerArtifactVariables(triggerArtifactVariables)
                                 .build())
                .build());
        break;
      case ORCHESTRATION:
        WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
        TriggerArgs wfTriggerArgs = workflowAction.getTriggerArgs();
        List<TriggerArtifactVariable> wfTriggerArtifactVariables =
            setTriggerArtifactVariables(deploymentTrigger.getAppId(), wfTriggerArgs.getTriggerArtifactVariables());
        deploymentTrigger.setAction(
            WorkflowAction.builder()
                .workflowId(workflowAction.getWorkflowId())
                .workflowName(
                    workflowService.fetchWorkflowName(deploymentTrigger.getAppId(), workflowAction.getWorkflowId()))
                .triggerArgs(TriggerArgs.builder()
                                 .excludeHostsWithSameArtifact(wfTriggerArgs.isExcludeHostsWithSameArtifact())
                                 .variables(wfTriggerArgs.getVariables())
                                 .triggerArtifactVariables(wfTriggerArtifactVariables)
                                 .build())
                .build());
        break;
      default:
        unhandled(deploymentTrigger.getAction().getActionType());
    }
  }

  private List<TriggerArtifactVariable> setTriggerArtifactVariables(
      String appId, List<TriggerArtifactVariable> inputTriggerArtifactVariables) {
    if (inputTriggerArtifactVariables != null) {
      return inputTriggerArtifactVariables.stream()
          .map(triggerArtifactVariable -> {
            if (triggerArtifactVariable.getType().equals(TriggerArtifactVariable.Type.ARTIFACT_SOURCE)
                && triggerArtifactVariable.getVariableValue() != null) {
              throw new WingsException("variable " + triggerArtifactVariable.getVariableName()
                  + " value should be empty for artifact source type");
            }
            return TriggerArtifactVariable.builder()
                .type(triggerArtifactVariable.getType())
                .variableName(triggerArtifactVariable.getVariableName())
                .variableValue(setVariableValue(appId, triggerArtifactVariable.getVariableValue()))
                .entityId(triggerArtifactVariable.getEntityId())
                .entityType(triggerArtifactVariable.getEntityType())
                .entityName(fetchEntityName(
                    appId, triggerArtifactVariable.getEntityId(), triggerArtifactVariable.getEntityType()))
                .build();
          })
          .collect(toList());
    } else {
      return null;
    }
  }

  private TriggerArtifactSelectionValue setVariableValue(String appId, TriggerArtifactSelectionValue inputValue) {
    if (inputValue == null) {
      return null;
    }

    switch (inputValue.getArtifactVariableType()) {
      case PIPELINE:
        TriggerArtifactSelectionPipeline pipelineVar = (TriggerArtifactSelectionPipeline) inputValue;

        return TriggerArtifactSelectionPipeline.builder()
            .pipelineId(pipelineVar.getPipelineId())
            .variableName(pipelineVar.getVariableName())
            .pipelineName(getPipelineName(appId, pipelineVar.getPipelineId()))
            .build();
      case ORCHESTRATION:
        TriggerArtifactSelectionWorkflow workflowVar = (TriggerArtifactSelectionWorkflow) inputValue;

        return TriggerArtifactSelectionWorkflow.builder()
            .workflowId(workflowVar.getWorkflowId())
            .workflowName(getWorkflowName(appId, workflowVar.getWorkflowId()))
            .variableName(workflowVar.getVariableName())
            .build();
      case ARTIFACT:
        TriggerArtifactSelectionArtifact artifactVar = (TriggerArtifactSelectionArtifact) inputValue;
        String artifactStreamId = artifactVar.getArtifactStreamId();
        ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);

        return TriggerArtifactSelectionArtifact.builder()
            .artifactFilter(artifactVar.getArtifactFilter())
            .artifactStreamId(artifactVar.getArtifactStreamId())
            .artifactSourceName(artifactStream.generateSourceName())
            .artifactStreamType(artifactStream.getArtifactStreamType())
            .build();
      default:
        unhandled(inputValue.getArtifactVariableType());
    }

    return null;
  }

  private String getPipelineName(String appId, String pipelineId) {
    return pipelineService.fetchPipelineName(appId, pipelineId);
  }
  private String getWorkflowName(String appId, String workflowId) {
    return workflowService.fetchWorkflowName(appId, workflowId);
  }

  private String fetchEntityName(String appId, String entityId, EntityType entityType) {
    switch (entityType) {
      case SERVICE:
        return serviceResourceService.get(entityId).getName();
      case ENVIRONMENT:
        return environmentService.get(appId, entityId, false).getName();
      case WORKFLOW:
        return workflowService.fetchWorkflowName(appId, entityId);
      default:
        unhandled(entityType);
    }

    return null;
  }

  private void validateTrigger(DeploymentTrigger trigger) {
    TriggerProcessor triggerProcessor = obtainTriggerProcessor(trigger);

    triggerProcessor.validateTriggerCondition(trigger);
    triggerServiceHelper.validateTriggerAction(trigger);
  }

  private TriggerProcessor obtainTriggerProcessor(DeploymentTrigger deploymentTrigger) {
    return triggerProcessorMapBinder.get(obtainTriggerConditionType(deploymentTrigger.getCondition()));
  }

  private String obtainTriggerConditionType(Condition condition) {
    if (condition.getType().equals(Type.NEW_ARTIFACT)) {
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

  private void setWorkflowName(DeploymentTrigger trigger) {
    switch (trigger.getAction().getActionType()) {
      case PIPELINE:
        PipelineAction pipelineAction = (PipelineAction) trigger.getAction();

        trigger.setAction(
            PipelineAction.builder()
                .pipelineId(pipelineAction.getPipelineId())
                .pipelineName(pipelineService.fetchPipelineName(trigger.getAppId(), pipelineAction.getPipelineId()))
                .triggerArgs(pipelineAction.getTriggerArgs())
                .build());
        break;
      case ORCHESTRATION:
        WorkflowAction workflowAction = (WorkflowAction) trigger.getAction();
        trigger.setAction(
            WorkflowAction.builder()
                .workflowId(workflowAction.getWorkflowId())
                .workflowName(workflowService.fetchWorkflowName(trigger.getAppId(), workflowAction.getWorkflowId()))
                .triggerArgs(workflowAction.getTriggerArgs())
                .build());
        break;
      default:
        unhandled(trigger.getAction().getActionType());
    }
  }
}
