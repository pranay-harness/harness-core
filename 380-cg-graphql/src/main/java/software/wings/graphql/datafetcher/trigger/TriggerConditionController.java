/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.FeatureName.WEBHOOK_TRIGGER_AUTHORIZATION;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import static software.wings.beans.trigger.WebhookSource.BitBucketEventType.ALL;
import static software.wings.beans.trigger.WebhookSource.BitBucketEventType.ANY;
import static software.wings.beans.trigger.WebhookSource.BitBucketEventType.PING;
import static software.wings.beans.trigger.WebhookSource.BitBucketEventType.valueOf;
import static software.wings.graphql.schema.type.trigger.QLGitHubAction.packageActions;
import static software.wings.graphql.schema.type.trigger.QLGitHubAction.pullRequestActions;
import static software.wings.graphql.schema.type.trigger.QLGitHubAction.releaseActions;

import io.harness.annotations.dev.OwnedBy;
import io.harness.configuration.DeployMode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;

import software.wings.app.MainConfiguration;
import software.wings.beans.SettingAttribute;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.ArtifactTriggerCondition.ArtifactTriggerConditionBuilder;
import software.wings.beans.trigger.GithubAction;
import software.wings.beans.trigger.ManifestTriggerCondition;
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
import software.wings.graphql.schema.type.trigger.QLGitHubAction;
import software.wings.graphql.schema.type.trigger.QLGitHubEvent;
import software.wings.graphql.schema.type.trigger.QLManifestConditionInput;
import software.wings.graphql.schema.type.trigger.QLOnNewArtifact;
import software.wings.graphql.schema.type.trigger.QLOnNewManifest;
import software.wings.graphql.schema.type.trigger.QLOnPipelineCompletion;
import software.wings.graphql.schema.type.trigger.QLOnSchedule;
import software.wings.graphql.schema.type.trigger.QLOnWebhook;
import software.wings.graphql.schema.type.trigger.QLTriggerCondition;
import software.wings.graphql.schema.type.trigger.QLTriggerConditionInput;
import software.wings.graphql.schema.type.trigger.QLTriggerConditionType;
import software.wings.graphql.schema.type.trigger.QLWebhookConditionInput;
import software.wings.graphql.schema.type.trigger.QLWebhookDetails;
import software.wings.graphql.schema.type.trigger.QLWebhookEvent;
import software.wings.graphql.schema.type.trigger.QLWebhookSource;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class TriggerConditionController {
  @Inject TriggerActionController triggerActionController;
  @Inject MainConfiguration mainConfiguration;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject SettingsService settingsService;
  @Inject ApplicationManifestService applicationManifestService;
  @Inject FeatureFlagService featureFlagService;

  public QLTriggerCondition populateTriggerCondition(Trigger trigger, String accountId) {
    QLTriggerCondition condition = null;
    String webhookUrl = null;

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

        webhookUrl = mainConfiguration.getPortal().getUrl().trim();
        // There is no gateway for On-prem
        // For SAAS, all calls should be routed via gateway:
        // https://harness.atlassian.net/browse/CDC-8897?focusedCommentId=126140
        if (!DeployMode.isOnPrem(mainConfiguration.getDeployMode().getDeployedAs())) {
          if (!webhookUrl.endsWith("/")) {
            webhookUrl += "/";
          }
          if (!webhookUrl.contains("gateway")) {
            webhookUrl += "gateway";
          }
        }

        boolean isManualTriggerAuthorized = featureFlagService.isEnabled(WEBHOOK_TRIGGER_AUTHORIZATION, accountId);
        String contentType = "content-type: application/json";

        webhookUrl +=
            "/api/webhooks/" + webHookTriggerCondition.getWebHookToken().getWebHookToken() + "?accountId=" + accountId;
        QLWebhookDetails details =
            QLWebhookDetails.builder()
                .webhookURL(webhookUrl)
                .header(isManualTriggerAuthorized ? contentType + ", x-api-key: x-api-key_placeholder" : contentType)
                .method(webHookTriggerCondition.getWebHookToken().getHttpMethod())
                .payload(webHookTriggerCondition.getWebHookToken().getPayload())
                .build();

        QLWebhookEvent event = null;
        switch (webhookSource) {
          case GITHUB:
            event = populateGithubEvent(trigger.getCondition());
            break;
          case GITLAB:
            event = populateGitlabEvent(trigger.getCondition());
            break;
          case BITBUCKET:
            event = populateBitbucketEvent(trigger.getCondition());
            break;
          default:
        }

        SettingAttribute gitConfig = settingsService.get(webHookTriggerCondition.getGitConnectorId());
        String gitConnectorName = gitConfig != null ? gitConfig.getName() : null;

        condition =
            QLOnWebhook.builder()
                .webhookSource(webhookSource)
                .webhookDetails(details)
                .webhookEvent(event)
                .branchRegex(webHookTriggerCondition.getBranchRegex())
                .branchName(webHookTriggerCondition.getBranchName())
                .repoName(webHookTriggerCondition.getRepoName())
                .gitConnectorId(webHookTriggerCondition.getGitConnectorId())
                .gitConnectorName(gitConnectorName)
                .filePaths(webHookTriggerCondition.getFilePaths())
                .deployOnlyIfFilesChanged(webHookTriggerCondition.isCheckFileContentChanged())
                .triggerConditionType(QLTriggerConditionType.valueOf(webHookTriggerCondition.getConditionType().name()))
                .webhookSecret(webHookTriggerCondition.getWebHookSecret())
                .build();

        break;
      case NEW_MANIFEST:
        ManifestTriggerCondition manifestTriggerCondition = (ManifestTriggerCondition) trigger.getCondition();
        condition = QLOnNewManifest.builder()
                        .appManifestId(manifestTriggerCondition.getAppManifestId())
                        .appManifestName(manifestTriggerCondition.getAppManifestName())
                        .serviceId(manifestTriggerCondition.getServiceId())
                        .versionRegex(manifestTriggerCondition.getVersionRegex())
                        .triggerConditionType(
                            QLTriggerConditionType.valueOf(manifestTriggerCondition.getConditionType().name()))
                        .build();
        break;
      default:
    }
    return condition;
  }

  private QLWebhookEvent populateGithubEvent(TriggerCondition triggerCondition) {
    String action = null;
    String eventType = null;
    if (isNotEmpty(((WebHookTriggerCondition) triggerCondition).getEventTypes())) {
      eventType = ((WebHookTriggerCondition) triggerCondition).getEventTypes().get(0).getValue();
    }

    if (GitHubEventType.PULL_REQUEST.getValue().equals(eventType)
        || (GitHubEventType.PACKAGE.getValue().equals(eventType))) {
      if (!isEmpty(((WebHookTriggerCondition) triggerCondition).getActions())) {
        action = ((WebHookTriggerCondition) triggerCondition).getActions().get(0).getValue();
      }
    } else if (GitHubEventType.RELEASE.getValue().equals(eventType)) {
      if (!isEmpty(((WebHookTriggerCondition) triggerCondition).getReleaseActions())) {
        action = ((WebHookTriggerCondition) triggerCondition).getReleaseActions().get(0).getValue();
      }
    }

    return QLWebhookEvent.builder().action(action).event(eventType).build();
  }

  private QLWebhookEvent populateGitlabEvent(TriggerCondition triggerCondition) {
    List<WebhookEventType> eventTypes = ((WebHookTriggerCondition) triggerCondition).getEventTypes();
    String event = null;
    if (isNotEmpty(eventTypes)) {
      event = eventTypes.get(0).getValue();
    }
    return QLWebhookEvent.builder().event(event).build();
  }

  private QLWebhookEvent populateBitbucketEvent(TriggerCondition triggerCondition) {
    List<BitBucketEventType> bitBucketEvents = ((WebHookTriggerCondition) triggerCondition).getBitBucketEvents();
    if (isEmpty(bitBucketEvents)) {
      return QLWebhookEvent.builder().build();
    }
    String eventType;
    String action = null;
    BitBucketEventType bitBucketEventType = bitBucketEvents.get(0);

    if (Sets.newHashSet(ANY, PING, ALL).contains(bitBucketEventType)) {
      eventType = bitBucketEventType.getValue();
    } else {
      String[] eventAndAction = bitBucketEventType.getValue().split(":");
      eventType = eventAndAction[0];
      action = eventAndAction[1];
    }

    return QLWebhookEvent.builder().action(action).event(eventType).build();
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
      case ON_NEW_MANIFEST:
        triggerCondition = validateAndResolveOnNewManifestConditionType(qlCreateOrUpdateTriggerInput);
        break;
      default:
    }

    return triggerCondition;
  }

  TriggerCondition validateAndResolveOnNewArtifactConditionType(
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

  TriggerCondition validateAndResolveOnNewManifestConditionType(
      QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    QLManifestConditionInput manifestConditionInput =
        qlCreateOrUpdateTriggerInput.getCondition().getManifestConditionInput();

    validateManifestTriggerCondition(qlCreateOrUpdateTriggerInput);

    return ManifestTriggerCondition.builder()
        .appManifestId(manifestConditionInput.getAppManifestId())
        .versionRegex(manifestConditionInput.getVersionRegex())
        .build();
  }

  private void validateManifestTriggerCondition(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    QLTriggerConditionInput triggerConditionInput = qlCreateOrUpdateTriggerInput.getCondition();

    if (null == triggerConditionInput.getManifestConditionInput()) {
      throw new InvalidRequestException("ManifestConditionInput cannot not be null for On New Manifest Trigger", USER);
    }
    String appManifestId = triggerConditionInput.getManifestConditionInput().getAppManifestId();

    if (EmptyPredicate.isEmpty(appManifestId)) {
      throw new InvalidRequestException("Application Manifest Id must not be null nor empty", USER);
    }

    ApplicationManifest applicationManifest =
        applicationManifestService.getById(qlCreateOrUpdateTriggerInput.getApplicationId(), appManifestId);
    if (applicationManifest == null) {
      throw new InvalidRequestException(
          String.format("Application manifest with id %s not found in given application %s", appManifestId,
              qlCreateOrUpdateTriggerInput.getApplicationId()),
          USER);
    }
  }

  TriggerCondition validateAndResolveOnPipelineCompletionConditionType(
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

  TriggerCondition validateAndResolveOnScheduleConditionType(
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

  TriggerCondition validateAndResolveOnWebhookConditionType(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    QLTriggerConditionInput triggerConditionInput = qlCreateOrUpdateTriggerInput.getCondition();

    if (null == triggerConditionInput.getWebhookConditionInput()) {
      throw new InvalidRequestException("WebhookConditionInput must not be null", USER);
    }
    return resolveWebhookTriggerCondition(triggerConditionInput);
  }

  void validateArtifactConditionArtifactStream(QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput) {
    QLTriggerConditionInput triggerConditionInput = qlCreateOrUpdateTriggerInput.getCondition();

    if (null == triggerConditionInput.getArtifactConditionInput()) {
      throw new InvalidRequestException("ArtifactConditionInput must not be null", USER);
    }
    String artifactSourceId = triggerConditionInput.getArtifactConditionInput().getArtifactSourceId();

    if (isEmpty(artifactSourceId)) {
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

  TriggerCondition resolveWebhookTriggerCondition(QLTriggerConditionInput qlTriggerConditionInput) {
    WebHookTriggerConditionBuilder builder = WebHookTriggerCondition.builder();
    QLWebhookConditionInput qlWebhookConditionInput = qlTriggerConditionInput.getWebhookConditionInput();

    switch (qlWebhookConditionInput.getWebhookSourceType()) {
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

    Boolean deployOnlyIfFilesChanged = qlWebhookConditionInput.getDeployOnlyIfFilesChanged();

    if (deployOnlyIfFilesChanged != null && deployOnlyIfFilesChanged) {
      builder.checkFileContentChanged(true);
      builder.gitConnectorId(qlWebhookConditionInput.getGitConnectorId());
      builder.filePaths(qlWebhookConditionInput.getFilePaths());
      builder.repoName(qlWebhookConditionInput.getRepoName());
      builder.branchName(qlWebhookConditionInput.getBranchName());
    }
    builder.branchRegex(qlTriggerConditionInput.getWebhookConditionInput().getBranchRegex());

    if (QLWebhookSource.CUSTOM != qlWebhookConditionInput.getWebhookSourceType()) {
      builder.webhookSource(WebhookSource.valueOf(qlWebhookConditionInput.getWebhookSourceType().name()));
    }
    builder.webHookSecret(qlWebhookConditionInput.getWebhookSecret());
    return builder.build();
  }

  private void resolveGitHubEvent(
      QLTriggerConditionInput qlTriggerConditionInput, WebHookTriggerConditionBuilder builder) {
    QLGitHubEvent githubEvent = qlTriggerConditionInput.getWebhookConditionInput().getGithubEvent();
    if (githubEvent == null || githubEvent.getEvent() == null) {
      throw new InvalidRequestException("Github event must not be null", USER);
    }

    List<WebhookEventType> eventTypes =
        Collections.singletonList(WebhookEventType.find(githubEvent.getEvent().name().toLowerCase()));
    builder.eventTypes(eventTypes);

    QLGitHubAction action = githubEvent.getAction();
    switch (githubEvent.getEvent()) {
      case PULL_REQUEST:
        if (!pullRequestActions.contains(action)) {
          throw new InvalidRequestException("Unsupported GitHub Action", USER);
        }
        builder.actions(Collections.singletonList(GithubAction.valueOf(action.name())));
        break;
      case RELEASE:
        if (!releaseActions.contains(action)) {
          throw new InvalidRequestException("Unsupported GitHub Release Action", USER);
        }
        builder.releaseActions(Collections.singletonList(ReleaseAction.valueOf(action.name())));
        break;
      case PACKAGE:
        if (!packageActions.contains(action)) {
          throw new InvalidRequestException("Unsupported GitHub Package Action", USER);
        }
        builder.actions(Collections.singletonList(GithubAction.valueOf(action.name())));
        break;
      default:
        // no actions for other event types.
    }
  }

  private void resolveGitLabEvent(
      QLTriggerConditionInput qlTriggerConditionInput, WebHookTriggerConditionBuilder builder) {
    if (qlTriggerConditionInput.getWebhookConditionInput().getGitlabEvent() == null) {
      throw new InvalidRequestException("Gitlab event must not be null", USER);
    }
    List<WebhookEventType> eventTypes = Arrays.asList(
        WebhookEventType.valueOf(qlTriggerConditionInput.getWebhookConditionInput().getGitlabEvent().name()));

    builder.eventTypes(eventTypes);
  }

  private void resolveBitBucketEvent(
      QLTriggerConditionInput qlTriggerConditionInput, WebHookTriggerConditionBuilder builder) {
    if (qlTriggerConditionInput.getWebhookConditionInput().getBitbucketEvent() == null) {
      throw new InvalidRequestException("Bitbucket event must not be null", USER);
    }
    BitBucketEventType bitBucketEventType =
        valueOf(qlTriggerConditionInput.getWebhookConditionInput().getBitbucketEvent().name());
    String webhookEventType = bitBucketEventType.getEventType().getValue();

    builder.eventTypes(Arrays.asList(WebhookEventType.find(webhookEventType)));
    builder.bitBucketEvents(Arrays.asList(bitBucketEventType));
  }
}
