package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.ArtifactTriggerCondition.ArtifactTriggerConditionBuilder;
import software.wings.beans.trigger.GithubAction;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ReleaseAction;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition.ScheduledTriggerConditionBuilder;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebHookTriggerCondition.WebHookTriggerConditionBuilder;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;
import software.wings.graphql.schema.type.trigger.QLCreateOrUpdateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLOnNewArtifact;
import software.wings.graphql.schema.type.trigger.QLOnPipelineCompletion;
import software.wings.graphql.schema.type.trigger.QLOnSchedule;
import software.wings.graphql.schema.type.trigger.QLOnWebhook;
import software.wings.graphql.schema.type.trigger.QLTriggerCondition;
import software.wings.graphql.schema.type.trigger.QLTriggerConditionInput;
import software.wings.graphql.schema.type.trigger.QLTriggerConditionType;
import software.wings.graphql.schema.type.trigger.QLWebhookDetails;
import software.wings.graphql.schema.type.trigger.QLWebhookEvent;
import software.wings.graphql.schema.type.trigger.QLWebhookSource;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.Arrays;
import java.util.List;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class TriggerConditionController {
  @Inject TriggerActionController triggerActionController;
  @Inject MainConfiguration mainConfiguration;
  @Inject ArtifactStreamService artifactStreamService;

  public QLTriggerCondition populateTriggerCondition(Trigger trigger, String accountId) {
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

  public TriggerCondition resolveTriggerCondition(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    TriggerCondition triggerCondition = null;
    switch (qlCreateOrUpdateTriggerInput.getCondition().getConditionType()) {
      case ON_NEW_ARTIFACT:
        triggerCondition = validateAndResolveOnNewArtifactConditionType(qlCreateOrUpdateTriggerInput);
        break;
      case ON_PIPELINE_COMPLETION:
        triggerCondition = validateAndResolveOnPipelineCompletionConditionType(qlCreateOrUpdateTriggerInput);
        break;
      case ON_SCHEDULE:
        triggerCondition = validateAndResolveOnScheduleConditionType(qlCreateOrUpdateTriggerInput);
        break;
      case ON_WEBHOOK:
        triggerCondition = validateAndResolveOnWebhookConditionType(qlCreateOrUpdateTriggerInput);
        break;
      default:
    }

    return triggerCondition;
  }

  private TriggerCondition validateAndResolveOnNewArtifactConditionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    QLTriggerConditionInput triggerConditionInput = qlCreateOrUpdateTriggerInput.getCondition();

    validateArtifactConditionArtifactStream(qlCreateOrUpdateTriggerInput);

    ArtifactTriggerConditionBuilder artifactTriggerConditionBuilder = ArtifactTriggerCondition.builder();
    artifactTriggerConditionBuilder.artifactStreamId(
        triggerConditionInput.getArtifactConditionInput().getArtifactSourceId());
    artifactTriggerConditionBuilder.artifactFilter(
        triggerConditionInput.getArtifactConditionInput().getArtifactFilter());

    if (triggerConditionInput.getArtifactConditionInput().getRegex() != null) {
      artifactTriggerConditionBuilder.regex(triggerConditionInput.getArtifactConditionInput().getRegex());
    }
    return artifactTriggerConditionBuilder.build();
  }

  private TriggerCondition validateAndResolveOnPipelineCompletionConditionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    QLTriggerConditionInput triggerConditionInput = qlCreateOrUpdateTriggerInput.getCondition();

    if (null == triggerConditionInput.getPipelineConditionInput()) {
      throw new InvalidRequestException("PipelineConditionInput must not be null", USER);
    }
    triggerActionController.validatePipeline(
        qlCreateOrUpdateTriggerInput, triggerConditionInput.getPipelineConditionInput().getPipelineId());

    return PipelineTriggerCondition.builder()
        .pipelineId(triggerConditionInput.getPipelineConditionInput().getPipelineId())
        .build();
  }

  private TriggerCondition validateAndResolveOnScheduleConditionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    QLTriggerConditionInput triggerConditionInput = qlCreateOrUpdateTriggerInput.getCondition();

    if (null == triggerConditionInput.getScheduleConditionInput()) {
      throw new InvalidRequestException("ScheduleConditionInput must not be null", USER);
    }

    ScheduledTriggerConditionBuilder scheduledTriggerConditionBuilder = ScheduledTriggerCondition.builder();
    scheduledTriggerConditionBuilder.cronExpression(
        triggerConditionInput.getScheduleConditionInput().getCronExpression());

    if (triggerConditionInput.getScheduleConditionInput().getOnNewArtifactOnly() != null) {
      scheduledTriggerConditionBuilder.onNewArtifactOnly(
          triggerConditionInput.getScheduleConditionInput().getOnNewArtifactOnly());
    }
    return scheduledTriggerConditionBuilder.build();
  }

  private TriggerCondition validateAndResolveOnWebhookConditionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    QLTriggerConditionInput triggerConditionInput = qlCreateOrUpdateTriggerInput.getCondition();

    if (null == triggerConditionInput.getWebhookConditionInput()) {
      throw new InvalidRequestException("WebhookConditionInput must not be null", USER);
    }
    return resolveWebhookTriggerCondition(triggerConditionInput);
  }

  private void validateArtifactConditionArtifactStream(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    QLTriggerConditionInput triggerConditionInput = qlCreateOrUpdateTriggerInput.getCondition();

    if (null == triggerConditionInput.getArtifactConditionInput()) {
      throw new InvalidRequestException("ArtifactConditionInput must not be null", USER);
    }
    String artifactSourceId = triggerConditionInput.getArtifactConditionInput().getArtifactSourceId();

    if (EmptyPredicate.isEmpty(artifactSourceId)) {
      throw new InvalidRequestException("ArtifactSource must not be null nor empty", USER);
    }

    ArtifactStream artifactStream = artifactStreamService.get(artifactSourceId);
    if (artifactStream != null) {
      if (!qlCreateOrUpdateTriggerInput.getApplicationId().equals(artifactStream.getAppId())) {
        throw new InvalidRequestException("Artifact Stream doesn't belong to this application", USER);
      }
    } else {
      throw new InvalidRequestException("Artifact Stream doesn't exist", USER);
    }
  }

  private TriggerCondition resolveWebhookTriggerCondition(QLTriggerConditionInput qlTriggerConditionInput) {
    WebHookTriggerConditionBuilder builder = WebHookTriggerCondition.builder();

    switch (qlTriggerConditionInput.getWebhookConditionInput().getWebhookSourceType()) {
      case GITHUB:
        resolveGitHubEvent(qlTriggerConditionInput, builder);
        break;
      case GITLAB:
        resolveGitLabEvent(qlTriggerConditionInput, builder);
        break;
      case BITBUCKET:
        resolveBitBucketEvent(qlTriggerConditionInput, builder);
        break;
      default:
    }

    builder.branchRegex(qlTriggerConditionInput.getWebhookConditionInput().getBranchRegex());
    if (QLWebhookSource.CUSTOM != qlTriggerConditionInput.getWebhookConditionInput().getWebhookSourceType()) {
      builder.webhookSource(
          WebhookSource.valueOf(qlTriggerConditionInput.getWebhookConditionInput().getWebhookSourceType().name()));
    }
    return builder.build();
  }

  private void resolveGitHubEvent(
      QLTriggerConditionInput qlTriggerConditionInput, WebHookTriggerConditionBuilder builder) {
    List<GithubAction> githubActions = null;
    List<ReleaseAction> releaseActions = null;

    if (qlTriggerConditionInput.getWebhookConditionInput().getGithubEvent() == null
        || qlTriggerConditionInput.getWebhookConditionInput().getGithubEvent().getEvent() == null) {
      throw new InvalidRequestException("Github event must not be null", USER);
    }

    List<WebhookEventType> eventTypes = Arrays.asList(WebhookEventType.find(
        qlTriggerConditionInput.getWebhookConditionInput().getGithubEvent().getEvent().name().toLowerCase()));

    GithubAction gitHubAction = null;
    switch (qlTriggerConditionInput.getWebhookConditionInput().getGithubEvent().getEvent()) {
      case PULL_REQUEST:
        gitHubAction = GithubAction.find(
            qlTriggerConditionInput.getWebhookConditionInput().getGithubEvent().getAction().name().toLowerCase());

        if (gitHubAction == GithubAction.PACKAGE_PUBLISHED) {
          throw new InvalidRequestException("Unsupported GitHub Action", USER);
        }
        githubActions = Arrays.asList(gitHubAction);
        break;
      case RELEASE:
        ReleaseAction releaseAction = ReleaseAction.find(
            qlTriggerConditionInput.getWebhookConditionInput().getGithubEvent().getAction().name().toLowerCase());
        releaseActions = Arrays.asList(releaseAction);
        break;
      case PACKAGE:
        gitHubAction = GithubAction.find(qlTriggerConditionInput.getWebhookConditionInput()
                                             .getGithubEvent()
                                             .getAction()
                                             .name()
                                             .toLowerCase()
                                             .replace("_", ":"));

        if (gitHubAction != GithubAction.PACKAGE_PUBLISHED) {
          throw new InvalidRequestException("Unsupported GitHub Action", USER);
        }
        githubActions = Arrays.asList(gitHubAction);
        break;
      default:
    }
    builder.eventTypes(eventTypes);
    builder.actions(githubActions);
    builder.releaseActions(releaseActions);
  }

  private void resolveGitLabEvent(
      QLTriggerConditionInput qlTriggerConditionInput, WebHookTriggerConditionBuilder builder) {
    if (qlTriggerConditionInput.getWebhookConditionInput().getGitlabEvent() == null) {
      throw new InvalidRequestException("Gitlab event must not be null", USER);
    }
    List<WebhookEventType> eventTypes = Arrays.asList(WebhookEventType.find(
        qlTriggerConditionInput.getWebhookConditionInput().getGitlabEvent().name().toLowerCase()));

    builder.eventTypes(eventTypes);
  }

  private void resolveBitBucketEvent(
      QLTriggerConditionInput qlTriggerConditionInput, WebHookTriggerConditionBuilder builder) {
    if (qlTriggerConditionInput.getWebhookConditionInput().getBitbucketEvent() == null) {
      throw new InvalidRequestException("Bitbucket event must not be null", USER);
    }
    BitBucketEventType bitBucketEventType =
        BitBucketEventType.valueOf(qlTriggerConditionInput.getWebhookConditionInput().getBitbucketEvent().name());
    String webhookEvent = null;
    if (bitBucketEventType.getValue().split(":")[0].equals("pullrequest")) {
      webhookEvent = "pull_request";
    } else {
      webhookEvent = bitBucketEventType.getValue().split(":")[0];
    }
    builder.eventTypes(Arrays.asList(WebhookEventType.find(webhookEvent)));
    builder.bitBucketEvents(Arrays.asList(bitBucketEventType));
  }
}