package software.wings.service.impl.trigger;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static net.redhogs.cronparser.CronExpressionDescriptor.getDescription;
import static software.wings.beans.trigger.Condition.Type.PIPELINE_COMPLETION;
import static software.wings.beans.trigger.WebhookCustomExpression.suggestExpressions;
import static software.wings.beans.trigger.WebhookEventType.ISSUE;
import static software.wings.beans.trigger.WebhookEventType.PULL_REQUEST;
import static software.wings.beans.trigger.WebhookEventType.PUSH;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import net.redhogs.cronparser.DescriptionTypeEnum;
import net.redhogs.cronparser.I18nMessages;
import net.redhogs.cronparser.Options;
import org.apache.commons.lang3.StringUtils;
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
import software.wings.service.intfc.PipelineService;
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
        pipelineService.fetchPipelineName(trigger.getAppId(), pipelineAction.getPipelineId());
      } catch (WingsException exception) {
        throw new WingsException("Pipeline does not exist for pipeline id " + pipelineAction.getPipelineId());
      }

      validateTriggerArgs(trigger.getAppId(), pipelineAction.getTriggerArgs());
    } else if (action.getActionType() == ActionType.ORCHESTRATION) {
      WorkflowAction workflowAction = (WorkflowAction) action;
      try {
        workflowService.fetchWorkflowName(trigger.getAppId(), workflowAction.getWorkflowId());
      } catch (WingsException exception) {
        throw new WingsException("workflow does not exist for workflowId " + workflowAction.getWorkflowId());
      }

      validateTriggerArgs(trigger.getAppId(), workflowAction.getTriggerArgs());
    }
  }

  public void reBuildTriggerActionWithNames(DeploymentTrigger deploymentTrigger) {
    switch (deploymentTrigger.getAction().getActionType()) {
      case PIPELINE:
        PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
        TriggerArgs triggerArgs = pipelineAction.getTriggerArgs();
        List<TriggerArtifactVariable> triggerArtifactVariables =
            artifactVariableHandler.transformTriggerArtifactVariables(
                deploymentTrigger.getAppId(), triggerArgs.getTriggerArtifactVariables());

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
            artifactVariableHandler.transformTriggerArtifactVariables(
                deploymentTrigger.getAppId(), wfTriggerArgs.getTriggerArtifactVariables());
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

  public static String getCronDescription(String cronExpression) {
    try {
      String description =
          getDescription(DescriptionTypeEnum.FULL, cronExpression, new Options(), I18nMessages.DEFAULT_LOCALE);
      return StringUtils.lowerCase("" + description.charAt(0)) + description.substring(1);
    } catch (Exception e) {
      throw new WingsException("Invalid cron expression" + cronExpression, USER);
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
          if (events.containsKey(gitHubEventType.getEventType().name())) {
            WebhookSource.WebhookEventInfo webhookEventInfo = events.get(gitHubEventType.getEventType().name());
            if (gitHubEventType.getEventType().equals(PULL_REQUEST)) {
              webhookEventInfo.getSubEvents().add(WebhookSubEventInfo.builder()
                                                      .displayValue(gitHubEventType.getDisplayName())
                                                      .enumName(gitHubEventType.name())
                                                      .build());
            }
          } else {
            List<WebhookSubEventInfo> subEvents = new ArrayList<>();

            if (gitHubEventType.getEventType().equals(PULL_REQUEST)) {
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
          if (events.containsKey(bitBucketEventType.getEventType().name())) {
            WebhookSource.WebhookEventInfo webhookEventInfo = events.get(bitBucketEventType.getEventType().name());
            if (bitBucketEventType.getEventType().equals(PULL_REQUEST)
                || bitBucketEventType.getEventType().equals(ISSUE) || bitBucketEventType.getEventType().equals(PUSH)) {
              webhookEventInfo.getSubEvents().add(WebhookSubEventInfo.builder()
                                                      .displayValue(bitBucketEventType.getDisplayName())
                                                      .enumName(bitBucketEventType.name())
                                                      .build());
            }
          } else {
            List<WebhookSubEventInfo> subEvents = new ArrayList<>();

            if (bitBucketEventType.getEventType().equals(PULL_REQUEST)
                || bitBucketEventType.getEventType().equals(ISSUE) || bitBucketEventType.getEventType().equals(PUSH)) {
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
  }
}
