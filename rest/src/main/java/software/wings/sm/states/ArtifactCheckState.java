package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.artifact.Artifact.ContentStatus;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADED;
import static software.wings.beans.artifact.Artifact.ContentStatus.FAILED;
import static software.wings.beans.artifact.Artifact.ContentStatus.METADATA_ONLY;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.scheduler.ReminderNotifyResponse;
import software.wings.service.impl.DelayEventHelper;
import software.wings.service.impl.DelayEventNotifyData;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.CronUtil;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtifactCheckState extends State {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCheckState.class);

  @Inject private transient ArtifactService artifactService;
  @Inject private transient CronUtil cronUtil;
  @Inject private transient DelayEventHelper delayEventHelper;
  @Inject private transient FeatureFlagService featureFlagService;

  private static int DELAY_TIME_IN_SEC = 60;

  public ArtifactCheckState(String name) {
    super(name, StateType.ARTIFACT_CHECK.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    List<Artifact> artifacts = workflowStandardParams.getArtifacts();
    if (isEmpty(artifacts)) {
      return anExecutionResponse().withErrorMessage("Artifacts are not required.").build();
    }
    List<Artifact> failedArtifacts =
        artifacts.stream()
            .filter(artifact -> artifact.getStatus() == Status.FAILED || artifact.getStatus() == Status.ERROR)
            .collect(toList());

    if (!isEmpty(failedArtifacts)) {
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage("One or more artifacts: " + failedArtifacts + " are in failed status")
          .build();
    }

    List<Artifact> missingContents = new ArrayList<>();
    artifacts.forEach(artifact -> {
      if (artifact.getContentStatus() == DOWNLOADED) {
        return;
      }
      missingContents.add(artifact);
    });
    if (missingContents.isEmpty()) {
      return getExecutionResponse(artifacts);
    }
    List<String> artifactNamesForDownload = new ArrayList<>();
    List<String> correlationIds = new ArrayList<>();

    artifacts.forEach(artifact -> {
      // TODO : auto downloaded is not done well in artifact stream - temporarily using Constants
      ContentStatus artifactContentStatus = artifactService.getArtifactContentStatus(artifact);
      if (DOWNLOADED == artifactContentStatus || METADATA_ONLY == artifactContentStatus) {
        return;
      }
      // Artifact needs to be downloaded now
      artifactService.startArtifactCollection(context.getAppId(), artifact.getUuid());
      String resumeId = delayEventHelper.delay(DELAY_TIME_IN_SEC, ImmutableMap.of("artifactId", artifact.getUuid()));
      correlationIds.add(resumeId);
      artifactNamesForDownload.add(artifact.getDisplayName());
    });

    if (artifactNamesForDownload.isEmpty()) {
      return getExecutionResponse(artifacts);
    }

    logger.info("startArtifactCollection requested - artifactNamesForDownload: {}", artifactNamesForDownload);
    logger.info("Asynch correlationIds: {}", correlationIds);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(correlationIds)
        .withErrorMessage("Waiting for artifacts:" + artifactNamesForDownload + " to be downloaded")
        .build();
  }

  private ExecutionResponse getExecutionResponse(List<Artifact> artifacts) {
    return anExecutionResponse()
        .withErrorMessage(
            "All artifacts: " + artifacts.stream().map(Artifact::getDisplayName).collect(toList()) + " are available.")
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    List<String> artifactNamesForDownload = new ArrayList<>();
    List<String> correlationIds = new ArrayList<>();
    List<Artifact> failedArtifacts = new ArrayList<>();

    logger.info("Received handleAsyncResponse - response: {}", response);
    response.values().forEach(notifyResponseData -> {
      String artifactId = "";
      if (notifyResponseData instanceof ReminderNotifyResponse) {
        ReminderNotifyResponse reminderNotifyResponse = (ReminderNotifyResponse) notifyResponseData;
        artifactId = reminderNotifyResponse.getParameters().get("artifactId");
      } else if (notifyResponseData instanceof DelayEventNotifyData) {
        DelayEventNotifyData delayEventNotifyData = (DelayEventNotifyData) notifyResponseData;
        artifactId = delayEventNotifyData.getContext().get("artifactId");
      }

      Artifact artifact = artifactService.get(context.getAppId(), artifactId);
      if (artifact.getContentStatus() == DOWNLOADED) {
        return;
      }
      if (artifact.getContentStatus() == FAILED) {
        failedArtifacts.add(artifact);
        return;
      }

      String resumeId = delayEventHelper.delay(DELAY_TIME_IN_SEC, ImmutableMap.of("artifactId", artifact.getUuid()));
      correlationIds.add(resumeId);
      artifactNamesForDownload.add(artifact.getDisplayName());
    });

    if (!isEmpty(failedArtifacts)) {
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage("One or more artifacts: "
              + failedArtifacts.stream().map(Artifact::getDisplayName).collect(toList()) + " are in failed status")
          .build();
    }

    if (artifactNamesForDownload.isEmpty()) {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      List<Artifact> artifacts = workflowStandardParams.getArtifacts();
      return getExecutionResponse(artifacts);
    }

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(correlationIds)
        .withErrorMessage("Waiting for artifacts:" + artifactNamesForDownload + " to be downloaded")
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // TODO : abort the cron
  }
}
