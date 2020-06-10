package software.wings.graphql.schema.type.trigger;

import lombok.experimental.UtilityClass;
import software.wings.app.MainConfiguration;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;

@UtilityClass
public class TriggerConditionController {
  public static QLTriggerCondition populateTriggerCondition(
      Trigger trigger, MainConfiguration mainConfiguration, String accountId) {
    QLTriggerCondition condition = null;

    switch (trigger.getCondition().getConditionType()) {
      case NEW_ARTIFACT:
        ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
        condition = QLOnNewArtifact.builder()
                        .artifactSourceId(artifactTriggerCondition.getArtifactStreamId())
                        .artifactSourceName(artifactTriggerCondition.getArtifactSourceName())
                        .artifactFilter(artifactTriggerCondition.getArtifactFilter())
                        .regex(artifactTriggerCondition.isRegex())
                        .triggerConditionType(
                            QLTriggerConditionType.valueOf(artifactTriggerCondition.getConditionType().name()))
                        .build();
        break;
      case PIPELINE_COMPLETION:
        PipelineTriggerCondition pipelineTriggerCondition = (PipelineTriggerCondition) trigger.getCondition();
        condition = QLOnPipelineCompletion.builder()
                        .pipelineId(pipelineTriggerCondition.getPipelineId())
                        .pipelineName(pipelineTriggerCondition.getPipelineName())
                        .triggerConditionType(
                            QLTriggerConditionType.valueOf(pipelineTriggerCondition.getConditionType().name()))
                        .build();
        break;
      case SCHEDULED:
        ScheduledTriggerCondition scheduledTriggerCondition = (ScheduledTriggerCondition) trigger.getCondition();
        condition = QLOnSchedule.builder()
                        .cronDescription(scheduledTriggerCondition.getCronDescription())
                        .cronExpression(scheduledTriggerCondition.getCronExpression())
                        .onNewArtifactOnly(scheduledTriggerCondition.isOnNewArtifactOnly())
                        .triggerConditionType(
                            QLTriggerConditionType.valueOf(scheduledTriggerCondition.getConditionType().name()))
                        .build();
        break;
      case WEBHOOK:
        WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();

        QLWebhookSource webhookSource;
        if (null == webHookTriggerCondition.getWebhookSource()) {
          webhookSource = QLWebhookSource.CUSTOM;
        } else {
          webhookSource = QLWebhookSource.valueOf(webHookTriggerCondition.getWebhookSource().toString());
        }

        StringBuilder stringBuilder = new StringBuilder(mainConfiguration.getPortal().getUrl());
        stringBuilder.append("/api/webhooks/")
            .append(webHookTriggerCondition.getWebHookToken().getWebHookToken())
            .append("?accountId=")
            .append(accountId);

        QLWebhookDetails details = QLWebhookDetails.builder()
                                       .webhookURL(stringBuilder.toString())
                                       .header("content-type: application/json")
                                       .method(webHookTriggerCondition.getWebHookToken().getHttpMethod())
                                       .payload(webHookTriggerCondition.getWebHookToken().getPayload())
                                       .build();

        String eventType = null;
        String action = null;
        switch (webhookSource) {
          case GITHUB:
            eventType = ((WebHookTriggerCondition) trigger.getCondition()).getEventTypes().get(0).getValue();

            if (GitHubEventType.PULL_REQUEST.getValue().equals(eventType)
                || (GitHubEventType.PACKAGE.getValue().equals(eventType))) {
              action = ((WebHookTriggerCondition) trigger.getCondition()).getActions().get(0).getValue();
            } else if (GitHubEventType.RELEASE.getValue().equals(eventType)) {
              action = ((WebHookTriggerCondition) trigger.getCondition()).getReleaseActions().get(0).getValue();
            }
            break;
          case GITLAB:
            eventType = ((WebHookTriggerCondition) trigger.getCondition()).getEventTypes().get(0).getValue();
            break;
          case BITBUCKET:
            String[] eventAndAction =
                ((WebHookTriggerCondition) trigger.getCondition()).getBitBucketEvents().get(0).getValue().split(":");
            eventType = eventAndAction[0];
            action = eventAndAction[1];
            break;
          default:
        }

        QLWebhookEvent event = QLWebhookEvent.builder().action(action).event(eventType).build();
        condition =
            QLOnWebhook.builder()
                .webhookSource(webhookSource)
                .webhookDetails(details)
                .webhookEvent(event)
                .branchRegex(webHookTriggerCondition.getBranchRegex())
                .triggerConditionType(QLTriggerConditionType.valueOf(webHookTriggerCondition.getConditionType().name()))
                .build();
        break;
      default:
    }
    return condition;
  }
}
