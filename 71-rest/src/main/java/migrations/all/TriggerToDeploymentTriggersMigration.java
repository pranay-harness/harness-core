package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.trigger.TriggerLastDeployedType.PIPELINE;
import static software.wings.beans.trigger.TriggerLastDeployedType.WORKFLOW;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.beans.WorkflowType;
import io.harness.exception.TriggerException;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Account;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.Action;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.BitBucketPayloadSource;
import software.wings.beans.trigger.Condition;
import software.wings.beans.trigger.Condition.Type;
import software.wings.beans.trigger.CustomPayloadExpression;
import software.wings.beans.trigger.CustomPayloadSource;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.GitHubPayloadSource;
import software.wings.beans.trigger.GitLabsPayloadSource;
import software.wings.beans.trigger.GithubAction;
import software.wings.beans.trigger.PayloadSource;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.PipelineCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactSelectionFromPipelineSource;
import software.wings.beans.trigger.TriggerArtifactSelectionFromSource;
import software.wings.beans.trigger.TriggerArtifactSelectionLastCollected;
import software.wings.beans.trigger.TriggerArtifactSelectionLastDeployed;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.trigger.TriggerArtifactSelectionWebhook;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.trigger.TriggerConditionType;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.beans.trigger.WebhookGitParam;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;
import software.wings.beans.trigger.WebhookSource.GitLabEventType;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j
public class TriggerToDeploymentTriggersMigration implements Migration {
  @Inject private AccountService accountService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private TriggerService triggerService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private SettingsService settingsService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private DeploymentTriggerService deploymentTriggerService;
  private final String DEBUG_LINE = "TRIGGER_MIGRATION: ";
  private final String accountId = "zEaak-FLS425IEO7OLzMUg";

  @Override
  public void migrate() {
    logger.info(StringUtils.join(DEBUG_LINE, "Starting trigger migration for accountId ", accountId));
    Account account = accountService.get(accountId);
    if (account == null) {
      logger.info(StringUtils.join(DEBUG_LINE, "Account does not exist, accountId ", accountId));
      return;
    }

    List<String> appIds = appService.getAppIdsByAccountId(accountId);
    for (String appId : appIds) {
      logger.info(StringUtils.join(DEBUG_LINE, "Starting migration for appId ", appId));

      List<Trigger> oldTriggers =
          triggerService.list(aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, appId).build(), false, null)
              .getResponse();

      logger.info(StringUtils.join(DEBUG_LINE, "Migrating triggers:  ", oldTriggers.size()));
      for (Trigger trigger : oldTriggers) {
        try {
          migrate(trigger);
        } catch (Exception e) {
          logger.error(StringUtils.join(DEBUG_LINE, "Migration failed for triggerId: " + trigger.getUuid()), e);
        }
      }
    }
  }

  private void migrate(Trigger trigger) {
    // delete existing deployemnt trigger with same name if exists already, to make the migration idempotent. Once
    // validated correctly in QA, will just skip these entities.
    List<DeploymentTrigger> existingTrigger = deploymentTriggerService.list(
        aPageRequest().addFilter("name", EQ, trigger.getName()).addFilter("appId", EQ, trigger.getAppId()).build(),
        false, null);
    if (isNotEmpty(existingTrigger)) {
      deploymentTriggerService.delete(trigger.getAppId(), existingTrigger.get(0).getUuid());
    }

    DeploymentTrigger deploymentTrigger = DeploymentTrigger.builder()
                                              .name(trigger.getName())
                                              .description(trigger.getDescription())
                                              .appId(trigger.getAppId())
                                              .accountId(accountId)
                                              .triggerDisabled(false)
                                              .webHookToken(trigger.getWebHookToken())
                                              .build();

    TriggerCondition oldCondition = trigger.getCondition();
    TriggerConditionType conditionType = oldCondition.getConditionType();
    Condition newCondition = constructNewCondition(trigger, deploymentTrigger, oldCondition, conditionType);
    deploymentTrigger.setCondition(newCondition);
    WorkflowType actionType = trigger.getWorkflowType();
    Action triggerAction = null;
    List<TriggerArtifactVariable> triggerArtifactVariables = new ArrayList<>();
    TriggerArgs triggerArgs;
    List<Variable> triggerVariables = new ArrayList<>();
    switch (actionType) {
      case ORCHESTRATION:
        Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), trigger.getWorkflowId());
        notNullCheck(StringUtils.join(DEBUG_LINE, "Workflow not found for Id", trigger.getWorkflowId(),
                         "Trigger: ", trigger.getUuid()),
            workflow);
        notNullCheck(StringUtils.join(DEBUG_LINE, "Orchestration Workflow not found for Id", trigger.getWorkflowId(),
                         "Trigger: ", trigger.getUuid()),
            workflow.getOrchestrationWorkflow());
        getArtifactVariables(actionType, trigger, triggerArtifactVariables);
        getTriggerVariablesWorkflow(trigger, triggerVariables, workflow);
        triggerArgs = TriggerArgs.builder()
                          .triggerArtifactVariables(triggerArtifactVariables)
                          .excludeHostsWithSameArtifact(trigger.isExcludeHostsWithSameArtifact())
                          .variables(triggerVariables)
                          .build();
        triggerAction = WorkflowAction.builder().workflowId(trigger.getWorkflowId()).triggerArgs(triggerArgs).build();
        break;
      case PIPELINE:
        Pipeline pipeline = pipelineService.readPipelineWithVariables(trigger.getAppId(), trigger.getWorkflowId());
        notNullCheck(StringUtils.join(DEBUG_LINE, "Pipeline not found for Id", trigger.getWorkflowId(),
                         "Trigger: ", trigger.getUuid()),
            pipeline);
        getArtifactVariables(actionType, trigger, triggerArtifactVariables);
        getTriggerVariablesPipeline(trigger, triggerVariables, pipeline);
        triggerArgs = TriggerArgs.builder()
                          .triggerArtifactVariables(triggerArtifactVariables)
                          .excludeHostsWithSameArtifact(trigger.isExcludeHostsWithSameArtifact())
                          .variables(triggerVariables)
                          .build();
        triggerAction = PipelineAction.builder().pipelineId(trigger.getWorkflowId()).triggerArgs(triggerArgs).build();
        break;
      default:
        logger.error(StringUtils.join(DEBUG_LINE, "Action type not supported for trigger: ", trigger.getUuid()));
    }

    deploymentTrigger.setAction(triggerAction);
    deploymentTriggerService.save(deploymentTrigger, true);
  }

  private Condition constructNewCondition(Trigger trigger, DeploymentTrigger deploymentTrigger,
      TriggerCondition oldCondition, TriggerConditionType conditionType) {
    Condition newCondition;
    switch (conditionType) {
      case SCHEDULED:
        ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) oldCondition;
        newCondition = ScheduledCondition.builder()
                           .onNewArtifactOnly(scheduledTriggerCondition.isOnNewArtifactOnly())
                           .cronExpression(scheduledTriggerCondition.getCronExpression())
                           .cronDescription(scheduledTriggerCondition.getCronDescription())
                           .build();
        deploymentTrigger.setType(Type.SCHEDULED);
        break;
      case NEW_ARTIFACT:
        ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) oldCondition;
        String artifactStreamId = artifactTriggerCondition.getArtifactStreamId();
        ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
        notNullCheck(StringUtils.join(DEBUG_LINE, "Artifact stream not found for Id", artifactStreamId,
                         "Trigger: ", trigger.getUuid()),
            artifactStream);

        String artifactServerId = artifactStream.getSettingId();
        SettingAttribute artifactServer = settingsService.get(artifactServerId);
        notNullCheck(StringUtils.join(DEBUG_LINE, "Artifact server not found for Id", artifactServerId,
                         "Trigger: ", trigger.getUuid()),
            artifactServer);

        newCondition = ArtifactCondition.builder()
                           .artifactStreamId(artifactTriggerCondition.getArtifactStreamId())
                           .artifactFilter(getArtifactFilter(artifactTriggerCondition))
                           .artifactServerId(artifactServer.getUuid())
                           .build();
        deploymentTrigger.setType(Type.NEW_ARTIFACT);
        break;
      case PIPELINE_COMPLETION:
        PipelineTriggerCondition pipelineTriggerCondition = (PipelineTriggerCondition) oldCondition;
        String pipelineId = pipelineTriggerCondition.getPipelineId();
        Pipeline pipeline = pipelineService.readPipeline(trigger.getAppId(), pipelineId, false);
        notNullCheck(
            StringUtils.join(DEBUG_LINE, "Pipeline not found for Id", pipelineId, "Trigger: ", trigger.getUuid()),
            pipeline);

        newCondition = PipelineCondition.builder().pipelineId(pipelineId).build();
        deploymentTrigger.setType(Type.PIPELINE_COMPLETION);

        break;
      case WEBHOOK:
        WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) oldCondition;
        PayloadSource payloadSource = getPayloadSource(webHookTriggerCondition, trigger);
        WebHookToken oldToken = webHookTriggerCondition.getWebHookToken();
        WebHookToken webHookToken =
            WebHookToken.builder()
                .httpMethod(oldToken.getHttpMethod())
                .webHookToken(oldToken.getWebHookToken())
                .payload(getNewPayload(oldToken.getPayload(), webHookTriggerCondition, trigger.getUuid()))
                .build();
        newCondition = WebhookCondition.builder().webHookToken(webHookToken).payloadSource(payloadSource).build();
        deploymentTrigger.setType(Type.WEBHOOK);
        break;
      default:
        throw new TriggerException(
            StringUtils.join(DEBUG_LINE, "Condition type not supported for trigger: ", trigger.getUuid()), null);
    }
    return newCondition;
  }

  private String getNewPayload(String payload, WebHookTriggerCondition webHookTriggerCondition, String triggerId) {
    WebhookSource webhookSource = webHookTriggerCondition.getWebhookSource();
    ObjectMapper mapper = new ObjectMapper();
    if (webhookSource == null) {
      try {
        WebHookRequest webHookRequest = mapper.readValue(payload, WebHookRequest.class);
        Map<String, String> parameters = webHookRequest.getParameters();
        Map<String, String> newParams = new HashMap<>();
        parameters.forEach((key, value) -> newParams.put(key, StringUtils.join("${", key, "}")));
        return mapper.writeValueAsString(newParams);
      } catch (IOException e) {
        throw new TriggerException(
            StringUtils.join(DEBUG_LINE, "Error migrating payload of trigger: ", triggerId), null);
      }
    }
    return "";
  }

  private PayloadSource getPayloadSource(WebHookTriggerCondition webHookTriggerCondition, Trigger trigger) {
    WebhookSource webhookSource = webHookTriggerCondition.getWebhookSource();
    if (webhookSource == null) {
      return CustomPayloadSource.builder().build();
    }
    List<CustomPayloadExpression> customPayloadExpressions = new ArrayList<>();
    String branchNameRegex = webHookTriggerCondition.getBranchRegex();

    switch (webhookSource) {
      case BITBUCKET:
        addBranchNameExpressionBitbucket(customPayloadExpressions, branchNameRegex);
        return BitBucketPayloadSource.builder()
            .bitBucketEvents(webHookTriggerCondition.getBitBucketEvents())
            .customPayloadExpressions(customPayloadExpressions)
            .build();
      case GITHUB:
        List<GithubAction> actions = webHookTriggerCondition.getActions();
        List<GitHubEventType> gitHubEventTypes =
            actions.stream().map(t -> GitHubEventType.find(t.getValue())).collect(Collectors.toList());

        WebhookGitParam webhookGitParam = WebhookGitParam.builder()
                                              .filePaths(webHookTriggerCondition.getFilePaths())
                                              .gitConnectorId(webHookTriggerCondition.getGitConnectorId())
                                              .branchName(branchNameRegex)
                                              .build();
        addBranchNameExpressionGithub(customPayloadExpressions, branchNameRegex);
        return GitHubPayloadSource.builder()
            .gitHubEventTypes(gitHubEventTypes)
            .webhookGitParam(webhookGitParam)
            .customPayloadExpressions(customPayloadExpressions)
            .build();
      case GITLAB:
        List<GitLabEventType> gitLabEventTypes = webHookTriggerCondition.getEventTypes()
                                                     .stream()
                                                     .map(t -> GitLabEventType.valueOf(t.name()))
                                                     .collect(Collectors.toList());
        addBranchNameExpressionGitLab(customPayloadExpressions, branchNameRegex);

        return GitLabsPayloadSource.builder()
            .gitLabEventTypes(gitLabEventTypes)
            .customPayloadExpressions(customPayloadExpressions)
            .build();
      default:
        return null;
    }
  }

  private void addBranchNameExpressionGitLab(
      List<CustomPayloadExpression> customPayloadExpressions, String branchNameRegex) {}

  private void addBranchNameExpressionGithub(
      List<CustomPayloadExpression> customPayloadExpressions, String branchNameRegex) {
    // todo
  }

  private void addBranchNameExpressionBitbucket(
      List<CustomPayloadExpression> customPayloadExpressions, String branchNameRegex) {
    // todo
  }

  private void getTriggerVariablesPipeline(Trigger trigger, List<Variable> triggerVariables, Pipeline pipeline) {
    if (isEmpty(trigger.getWorkflowVariables())) {
      // nothing to migrate
      return;
    }
    Map<String, String> workflowVariables = trigger.getWorkflowVariables();
    List<Variable> pipelineVariables = pipeline.getPipelineVariables();
    if (isEmpty(pipelineVariables)) {
      logger.info(StringUtils.join(DEBUG_LINE, "Pipeline is no more templatised, workflowId: ", pipeline.getUuid(),
          " trigger: ", trigger.getUuid()));
      return;
    }
    for (Entry<String, String> variable : workflowVariables.entrySet()) {
      if (isEmpty(variable.getValue())) {
        continue;
      }
      Variable var =
          pipelineVariables.stream().filter(t -> t.getName().equals(variable.getKey())).findFirst().orElse(null);
      if (var != null) {
        notNullCheck("Condition cannot be null", trigger.getCondition());
        var.setValue(getVariableValue(variable, trigger.getCondition().getConditionType()));
        triggerVariables.add(var);
      }
    }
  }

  private void getTriggerVariablesWorkflow(Trigger trigger, List<Variable> triggerVariables, Workflow workflow) {
    if (isNotEmpty(trigger.getWorkflowVariables())) {
      Map<String, String> workflowVariables = trigger.getWorkflowVariables();
      List<Variable> userVariables = workflow.getOrchestrationWorkflow().getUserVariables();
      if (isEmpty(userVariables)) {
        logger.info(StringUtils.join(DEBUG_LINE, "Workflow is no more templatised, workflowId: ", workflow.getUuid(),
            " trigger: ", trigger.getUuid()));
        return;
      }
      for (Entry<String, String> variable : workflowVariables.entrySet()) {
        if (isEmpty(variable.getValue())) {
          continue;
        }
        Variable var =
            userVariables.stream().filter(t -> t.getName().equals(variable.getKey())).findFirst().orElse(null);
        if (var != null) {
          notNullCheck("Condition cannot be null", trigger.getCondition());
          var.setValue(getVariableValue(variable, trigger.getCondition().getConditionType()));
          triggerVariables.add(var);
        }
      }
    }
  }

  private String getVariableValue(Entry<String, String> variable, TriggerConditionType condition) {
    return condition == TriggerConditionType.WEBHOOK && matchesVariablePattern(variable.getValue())
        ? StringUtils.join("${", variable.getKey(), "}")
        : variable.getValue();
    // Concrete values will be validated with trigger validation. And trigger should be marked invalid if validation
    // fails.
  }

  private void getArtifactVariables(
      WorkflowType actionType, Trigger trigger, List<TriggerArtifactVariable> triggerArtifactVariables) {
    if (isNotEmpty(trigger.getArtifactSelections())) {
      for (ArtifactSelection artifactSelection : trigger.getArtifactSelections()) {
        ArtifactSelection.Type artifactSelectiontype = artifactSelection.getType();
        notNullCheck(StringUtils.join(DEBUG_LINE, "Artifact selection type not found for Trigger: ", trigger.getUuid()),
            artifactSelectiontype);
        TriggerArtifactSelectionValue artifactSelectionValue = null;
        switch (artifactSelectiontype) {
          case LAST_DEPLOYED:
            if (actionType == WorkflowType.ORCHESTRATION) {
              String workflowId = artifactSelection.getWorkflowId();
              Workflow workflow = workflowService.readWorkflow(trigger.getAppId(), trigger.getWorkflowId());
              notNullCheck(StringUtils.join(DEBUG_LINE, "Workflow not found for Id in artifact selection",
                               trigger.getWorkflowId(), "Trigger: ", trigger.getUuid()),
                  workflow);
              artifactSelectionValue =
                  TriggerArtifactSelectionLastDeployed.builder().id(workflowId).type(WORKFLOW).build();
            } else {
              Pipeline pipeline =
                  pipelineService.readPipelineWithVariables(trigger.getAppId(), trigger.getWorkflowId());
              notNullCheck(StringUtils.join(DEBUG_LINE, "Pipeline not found for Id in artifact selection",
                               trigger.getWorkflowId(), "Trigger: ", trigger.getUuid()),
                  pipeline);
              // how to get WorkflowId from ServiceId and pipelineId: @Srinivas Todo
              artifactSelectionValue = TriggerArtifactSelectionLastDeployed.builder().id(null).type(PIPELINE).build();
            }

            break;
          case LAST_COLLECTED:
            String artifactStreamId = artifactSelection.getArtifactStreamId();
            ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
            notNullCheck(StringUtils.join(DEBUG_LINE, "Artifact stream not found for Id", artifactStreamId,
                             "Trigger: ", trigger.getUuid()),
                artifactStream);

            String artifactServerId = artifactStream.getSettingId();
            SettingAttribute artifactServer = settingsService.get(artifactServerId);
            notNullCheck(StringUtils.join(DEBUG_LINE, "Artifact server not found for Id", artifactServerId,
                             "Trigger: ", trigger.getUuid()),
                artifactServer);

            artifactSelectionValue = TriggerArtifactSelectionLastCollected.builder()
                                         .artifactStreamId(artifactSelection.getArtifactStreamId())
                                         .artifactServerId(artifactServerId)
                                         .artifactFilter(getArtifactFilter(artifactSelection))
                                         .build();
            break;
          case PIPELINE_SOURCE:
            artifactSelectionValue = TriggerArtifactSelectionFromPipelineSource.builder().build();
            break;
          case ARTIFACT_SOURCE:
            artifactSelectionValue = TriggerArtifactSelectionFromSource.builder().build();
            break;
          case WEBHOOK_VARIABLE:
            artifactStreamId = artifactSelection.getArtifactStreamId();
            artifactStream = artifactStreamService.get(artifactStreamId);
            notNullCheck(StringUtils.join(DEBUG_LINE, "Artifact stream not found for Id", artifactStreamId,
                             "Trigger: ", trigger.getUuid()),
                artifactStream);

            artifactServerId = artifactStream.getSettingId();
            artifactServer = settingsService.get(artifactServerId);
            notNullCheck(StringUtils.join(DEBUG_LINE, "Artifact server not found for Id", artifactServerId,
                             "Trigger: ", trigger.getUuid()),
                artifactServer);

            artifactSelectionValue = TriggerArtifactSelectionWebhook.builder()
                                         .artifactStreamId(artifactSelection.getArtifactStreamId())
                                         .artifactServerId(artifactServerId)
                                         .build();
            break;
          default:
            logger.error(
                StringUtils.join(DEBUG_LINE, "Artifact selection type not supported for trigger: ", trigger.getUuid()));
        }

        notNullCheck(StringUtils.join(DEBUG_LINE,
                         "Trigger artifact variable value null after calculation for trigger: ", trigger.getUuid()),
            artifactSelectionValue);
        TriggerArtifactVariable triggerArtifactVariable = TriggerArtifactVariable.builder()
                                                              .variableName("artifact")
                                                              .entityType(EntityType.SERVICE)
                                                              .entityId(artifactSelection.getServiceId())
                                                              .variableValue(artifactSelectionValue)
                                                              .build();
        triggerArtifactVariables.add(triggerArtifactVariable);
      }
    }
  }

  private String getArtifactFilter(ArtifactSelection artifactSelection) {
    if (artifactSelection.isRegex()) {
      return artifactSelection.getArtifactFilter();
    } else {
      if (isNotEmpty(artifactSelection.getArtifactFilter())) {
        return StringUtils.join("^", artifactSelection.getArtifactFilter(), "$");
      }
    }
    return null;
  }

  private String getArtifactFilter(ArtifactTriggerCondition artifactTriggerCondition) {
    if (artifactTriggerCondition.isRegex()) {
      return artifactTriggerCondition.getArtifactFilter();
    } else {
      if (isNotEmpty(artifactTriggerCondition.getArtifactFilter())) {
        return StringUtils.join("^", artifactTriggerCondition.getArtifactFilter(), "$");
      }
    }

    return null;
  }
}
