package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ContentStatus;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.scheduler.ReminderNotifyResponse;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
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
import java.util.stream.Collectors;

public class ArtifactCheckState extends State {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCheckState.class);

  @Inject private ArtifactStreamService artifactStreamService;

  @Inject private ArtifactService artifactService;

  @Inject private CronUtil cronUtil;

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
            .filter(artifact
                -> artifact.getStatus() == Status.FAILED || artifact.getStatus() == Status.ERROR
                    || artifact.getContentStatus() == ContentStatus.FAILED)
            .collect(Collectors.toList());

    if (!isEmpty(failedArtifacts)) {
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage("One or more artifacts: " + failedArtifacts + " are in failed status")
          .build();
    }

    List<Artifact> missingContents = new ArrayList<>();
    artifacts.forEach(artifact -> {
      if (artifact.getContentStatus() == ContentStatus.DOWNLOADED) {
        return;
      }
      missingContents.add(artifact);
    });
    if (missingContents.isEmpty()) {
      return anExecutionResponse()
          .withErrorMessage("All artifacts: "
              + artifacts.stream().map(Artifact::getDisplayName).collect(Collectors.toList()) + " are available.")
          .build();
    }
    List<String> artifactNamesForDownload = new ArrayList<>();
    List<String> correlationIds = new ArrayList<>();

    artifacts.forEach(artifact -> {
      ArtifactStream artifactStream = artifactStreamService.get(context.getAppId(), artifact.getArtifactStreamId());
      if (artifactStream == null) {
        // mean artifact stream has already been deleted
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Artifact Source: " + artifact.getArtifactSourceName() + " has already been deleted");
      }

      // TODO : auto downloaded is not done well in artifact stream - temporarily using Constants
      if (artifactStream.isMetadataOnly() || Constants.autoDownloaded.contains(artifactStream.getArtifactStreamType())
          || artifactService.getArtifactContentStatus(artifact).equals(ContentStatus.METADATA_ONLY)) {
        return;
      }

      // Artifact needs to be downloaded now
      artifactService.startArtifactCollection(context.getAppId(), artifact.getUuid());
      correlationIds.add(cronUtil.scheduleReminder(60 * 1000, "artifactId", artifact.getUuid()));
      artifactNamesForDownload.add(artifact.getDisplayName());
    });

    if (artifactNamesForDownload.isEmpty()) {
      return anExecutionResponse()
          .withErrorMessage("All artifacts: "
              + artifacts.stream().map(Artifact::getDisplayName).collect(Collectors.toList()) + " are available.")
          .build();
    }

    logger.info("startArtifactCollection requested - artifactNamesForDownload: {}", artifactNamesForDownload);
    logger.info("Asynch correlationIds: {}", correlationIds);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(correlationIds)
        .withErrorMessage("Waiting for artifacts:" + artifactNamesForDownload + " to be downloaded")
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    List<String> artifactNamesForDownload = new ArrayList<>();
    List<String> correlationIds = new ArrayList<>();
    List<Artifact> failedArtifacts = new ArrayList<>();

    logger.info("Received handleAsyncResponse - response: {}", response);
    response.values().forEach(notifyResponseData -> {
      ReminderNotifyResponse reminderNotifyResponse = (ReminderNotifyResponse) notifyResponseData;
      String artifactId = reminderNotifyResponse.getParameters().get("artifactId");
      Artifact artifact = artifactService.get(context.getAppId(), artifactId);
      if (artifact.getContentStatus() == ContentStatus.DOWNLOADED) {
        return;
      }
      if (artifact.getContentStatus() == ContentStatus.FAILED) {
        failedArtifacts.add(artifact);
        return;
      }

      correlationIds.add(cronUtil.scheduleReminder(60 * 1000, "artifactId", artifact.getUuid()));
      artifactNamesForDownload.add(artifact.getDisplayName());
    });

    if (!isEmpty(failedArtifacts)) {
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage("One or more artifacts: "
              + failedArtifacts.stream().map(Artifact::getDisplayName).collect(Collectors.toList())
              + " are in failed status")
          .build();
    }

    if (artifactNamesForDownload.isEmpty()) {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      List<Artifact> artifacts = workflowStandardParams.getArtifacts();
      return anExecutionResponse()
          .withErrorMessage("All artifacts: "
              + artifacts.stream().map(Artifact::getDisplayName).collect(Collectors.toList()) + " are available.")
          .build();
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
