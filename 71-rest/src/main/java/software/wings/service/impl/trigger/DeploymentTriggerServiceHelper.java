package software.wings.service.impl.trigger;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.govern.Switch.unhandled;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.trigger.Condition.Type.PIPELINE_COMPLETION;
import static software.wings.beans.trigger.WebhookCustomExpression.suggestExpressions;
import static software.wings.beans.trigger.WebhookEventType.ISSUE;
import static software.wings.beans.trigger.WebhookEventType.PULL_REQUEST;
import static software.wings.beans.trigger.WebhookEventType.PUSH;
import static software.wings.scheduler.ScheduledTriggerJob.PREFIX;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import net.redhogs.cronparser.I18nMessages;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.trigger.Action;
import software.wings.beans.trigger.Action.ActionType;
import software.wings.beans.trigger.Condition.Type;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.PipelineCondition;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;
import software.wings.beans.trigger.WebhookSource.GitLabEventType;
import software.wings.beans.trigger.WebhookSource.WebhookSubEventInfo;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.dl.WingsPersistence;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class DeploymentTriggerServiceHelper {
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient TriggerArtifactVariableHandler artifactVariableHandler;
  @Inject private transient PipelineService pipelineService;
  @Inject private transient WorkflowService workflowService;
  @Inject private transient EnvironmentService environmentService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient InfrastructureDefinitionService infrastructureDefinitionService;

  public List<DeploymentTrigger> getTriggersByApp(String appId, Type condition) {
    return wingsPersistence
        .query(DeploymentTrigger.class,
            aPageRequest().addFilter("type", EQ, condition).addFilter("appId", EQ, appId).build())
        .getResponse();
  }

  public void validateTriggerAction(DeploymentTrigger trigger) {
    Action action = trigger.getAction();
    if (action.getActionType() == ActionType.PIPELINE) {
      PipelineAction pipelineAction = (PipelineAction) action;
      try {
        if (pipelineAction.getPipelineId() == null) {
          throw new TriggerException("pipelineId is null for trigger " + trigger.getName(), null);
        }
        pipelineService.fetchPipelineName(trigger.getAppId(), pipelineAction.getPipelineId());
      } catch (WingsException exception) {
        throw new WingsException("Pipeline does not exist for pipeline id " + pipelineAction.getPipelineId());
      }

      validateTriggerArgs(trigger.getAppId(), pipelineAction.getTriggerArgs());
    } else if (action.getActionType() == ActionType.WORKFLOW) {
      WorkflowAction workflowAction = (WorkflowAction) action;
      try {
        if (workflowAction.getWorkflowId() == null) {
          throw new TriggerException("workflow Id is null for trigger " + trigger.getName(), null);
        }

        workflowService.fetchWorkflowName(trigger.getAppId(), workflowAction.getWorkflowId());
      } catch (WingsException exception) {
        throw new WingsException("workflow does not exist for workflowId " + workflowAction.getWorkflowId());
      }

      validateTriggerArgs(trigger.getAppId(), workflowAction.getTriggerArgs());
    }
  }

  public void reBuildTriggerActionWithNames(
      DeploymentTrigger deploymentTrigger, boolean readPrimaryVariablesValueNames) {
    switch (deploymentTrigger.getAction().getActionType()) {
      case PIPELINE:
        PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
        TriggerArgs triggerArgs = pipelineAction.getTriggerArgs();
        List<TriggerArtifactVariable> triggerArtifactVariables =
            artifactVariableHandler.transformTriggerArtifactVariables(
                deploymentTrigger.getAppId(), triggerArgs.getTriggerArtifactVariables());

        List<Variable> triggerVariables = triggerArgs.getVariables();
        if (isNotEmpty(triggerVariables) && readPrimaryVariablesValueNames) {
          validateAndTransformTriggerVariables(deploymentTrigger.getAppId(), triggerVariables);
        }

        deploymentTrigger.setAction(
            PipelineAction.builder()
                .pipelineId(pipelineAction.getPipelineId())
                .pipelineName(
                    pipelineService.fetchPipelineName(deploymentTrigger.getAppId(), pipelineAction.getPipelineId()))
                .triggerArgs(TriggerArgs.builder()
                                 .excludeHostsWithSameArtifact(triggerArgs.isExcludeHostsWithSameArtifact())
                                 .variables(triggerVariables)
                                 .triggerArtifactVariables(triggerArtifactVariables)
                                 .build())
                .build());
        break;
      case WORKFLOW:
        WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
        TriggerArgs wfTriggerArgs = workflowAction.getTriggerArgs();
        List<Variable> wfTriggerVariables = wfTriggerArgs.getVariables();
        if (isNotEmpty(wfTriggerVariables) && readPrimaryVariablesValueNames) {
          validateAndTransformTriggerVariables(deploymentTrigger.getAppId(), wfTriggerVariables);
        }
        List<TriggerArtifactVariable> wfTriggerArtifactVariables =
            artifactVariableHandler.transformTriggerArtifactVariables(
                deploymentTrigger.getAppId(), wfTriggerArgs.getTriggerArtifactVariables());
        deploymentTrigger.setAction(
            WorkflowAction.builder()
                .workflowId(workflowAction.getWorkflowId())
                .workflowName(
                    workflowService.fetchWorkflowName(deploymentTrigger.getAppId(), workflowAction.getWorkflowId()))
                .triggerArgs(TriggerArgs.builder()
                                 .excludeHostsWithSameArtifact(wfTriggerArgs.isExcludeHostsWithSameArtifact())
                                 .variables(wfTriggerVariables)
                                 .triggerArtifactVariables(wfTriggerArtifactVariables)
                                 .build())
                .build());
        break;
      default:
        unhandled(deploymentTrigger.getAction().getActionType());
    }
  }

  public static String getCronDescription(String cronExpression) {
    try {
      CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
      Cron cron = parser.parse(PREFIX + cronExpression);
      return CronDescriptor.instance(I18nMessages.DEFAULT_LOCALE).describe(cron);
    } catch (Exception e) {
      throw new TriggerException(e.getMessage(), USER);
    }
  }

  public Map<String, String> fetchCustomExpressionList(String webhookSource) {
    return suggestExpressions(webhookSource);
  }

  public Map<String, WebhookSource.WebhookEventInfo> fetchWebhookChildEvents(String webhookSource) {
    Map<String, WebhookSource.WebhookEventInfo> events = new HashMap<>();
    switch (webhookSource) {
      case "GITHUB":
        GitHubEventType.GHEventHolder.getMap().values().forEach(gitHubEventType -> {
          if (gitHubEventType.getEventType().name().equals("PING") || gitHubEventType.name().equals("PULL_REQUEST")) {
            return;
          }
          if (events.containsKey(gitHubEventType.getEventType().name())) {
            WebhookSource.WebhookEventInfo webhookEventInfo = events.get(gitHubEventType.getEventType().name());
            if (gitHubEventType.getEventType() == PULL_REQUEST) {
              webhookEventInfo.getSubEvents().add(WebhookSubEventInfo.builder()
                                                      .displayValue(gitHubEventType.getDisplayName())
                                                      .enumName(gitHubEventType.name())
                                                      .build());
            }
          } else {
            List<WebhookSubEventInfo> subEvents = new ArrayList<>();

            if (gitHubEventType.getEventType() == PULL_REQUEST) {
              subEvents.add(WebhookSubEventInfo.builder()
                                .displayValue(gitHubEventType.getDisplayName())
                                .enumName(gitHubEventType.name())
                                .build());
            }

            WebhookSource.WebhookEventInfo webhookEventInfo =
                WebhookSource.WebhookEventInfo.builder()
                    .displayValue(gitHubEventType.getEventType().getDisplayName())
                    .enumName(gitHubEventType.getEventType().name())
                    .subEvents(subEvents)
                    .build();

            events.put(gitHubEventType.getEventType().name(), webhookEventInfo);
          }
        });
        break;
      case "GITLAB":
        GitLabEventType.GitLabEventHolder.getMap().values().forEach(gitLabEventType -> {
          if (gitLabEventType.getEventType().name().equals("PING")
              || gitLabEventType.getEventType().name().equals("ANY")) {
            return;
          }
          WebhookSource.WebhookEventInfo webhookEventInfo =
              WebhookSource.WebhookEventInfo.builder()
                  .displayValue(gitLabEventType.getEventType().getDisplayName())
                  .enumName(gitLabEventType.getEventType().name())
                  .build();

          events.put(gitLabEventType.getEventType().name(), webhookEventInfo);
        });
        break;
      case "BITBUCKET":
        BitBucketEventType.BitBucketEventHolder.getMap().values().forEach(bitBucketEventType -> {
          if (bitBucketEventType.getEventType().name().equals("PING")) {
            return;
          }
          if (events.containsKey(bitBucketEventType.getEventType().name())) {
            WebhookSource.WebhookEventInfo webhookEventInfo = events.get(bitBucketEventType.getEventType().name());
            if (bitBucketEventType.getEventType() == PULL_REQUEST || bitBucketEventType.getEventType() == ISSUE
                || bitBucketEventType.getEventType() == PUSH) {
              webhookEventInfo.getSubEvents().add(WebhookSubEventInfo.builder()
                                                      .displayValue(bitBucketEventType.getDisplayName())
                                                      .enumName(bitBucketEventType.name())
                                                      .build());
            }
          } else {
            List<WebhookSubEventInfo> subEvents = new ArrayList<>();

            if (bitBucketEventType.getEventType() == PULL_REQUEST || bitBucketEventType.getEventType() == ISSUE
                || bitBucketEventType.getEventType() == PUSH) {
              subEvents.add(WebhookSubEventInfo.builder()
                                .displayValue(bitBucketEventType.getDisplayName())
                                .enumName(bitBucketEventType.name())
                                .build());
            }

            WebhookSource.WebhookEventInfo webhookEventInfo =
                WebhookSource.WebhookEventInfo.builder()
                    .displayValue(bitBucketEventType.getEventType().getDisplayName())
                    .enumName(bitBucketEventType.getEventType().name())
                    .subEvents(subEvents)
                    .build();

            events.put(bitBucketEventType.getEventType().name(), webhookEventInfo);
          }
        });
        break;

      default:
        unhandled(webhookSource);
        return null;
    }
    return events;
  }

  public Stream<DeploymentTrigger> getTriggersMatchesWorkflow(String appId, String sourcePipelineId) {
    return getTriggersByApp(appId, PIPELINE_COMPLETION)
        .stream()
        .filter(trigger -> ((PipelineCondition) trigger.getCondition()).getPipelineId().equals(sourcePipelineId));
  }

  private void validateTriggerArgs(String appId, TriggerArgs triggerArgs) {
    notNullCheck("Trigger args not exist ", triggerArgs, USER);
    List<TriggerArtifactVariable> triggerArtifactVariables = triggerArgs.getTriggerArtifactVariables();

    if (triggerArtifactVariables != null) {
      artifactVariableHandler.validateTriggerArtifactVariables(appId, triggerArtifactVariables);
    }

    List<Variable> triggerVariables = triggerArgs.getVariables();

    if (isNotEmpty(triggerVariables)) {
      validateAndTransformTriggerVariables(appId, triggerVariables);
    }
  }

  private void validateAndTransformTriggerVariables(String appId, List<Variable> triggerVariables) {
    if (isEmpty(triggerVariables)) {
      return;
    }
    for (Variable variable : triggerVariables) {
      EntityType entityType = variable.obtainEntityType();
      if (entityType != null) {
        switch (variable.obtainEntityType()) {
          case ENVIRONMENT:
            String envId = variable.getValue();
            if (!matchesVariablePattern(envId)) {
              Environment environment = environmentService.get(appId, envId);
              notNullCheck(
                  "Environment not found for Id: " + envId + "in value for var" + variable.getName(), environment);
              variable.getMetadata().put("variableDisplay", environment.getName());
            }
            break;
          case INFRASTRUCTURE_MAPPING:
            String infraMapId = variable.getValue();
            if (!matchesVariablePattern(infraMapId)) {
              InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMapId);
              notNullCheck("Infra Structure not found for Id: " + infraMapId + "in value for var" + variable.getName(),
                  infrastructureMapping);
              variable.getMetadata().put("variableDisplay", infrastructureMapping.getName());
            }
            break;
          case SERVICE:
            String serviceId = variable.getValue();
            if (!matchesVariablePattern(serviceId)) {
              Service service = serviceResourceService.get(appId, serviceId);
              notNullCheck("Service not found for Id: " + serviceId + "in value for var" + variable.getName(), service);
              variable.getMetadata().put("variableDisplay", service.getName());
            }
            break;
          case INFRASTRUCTURE_DEFINITION:
            String infraDefId = variable.getValue();
            if (!matchesVariablePattern(infraDefId)) {
              InfrastructureDefinition infrastructureDefinition =
                  infrastructureDefinitionService.get(appId, infraDefId);
              notNullCheck(
                  "Infrastructure Definition not found for Id: " + infraDefId + "in value for var" + variable.getName(),
                  infrastructureDefinition);
              variable.getMetadata().put("variableDisplay", infrastructureDefinition.getName());
            }
            break;
          default:
            logger.info("No need to transform. Not a primary variable");
        }
      }
    }
  }
}
