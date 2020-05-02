package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.artifact.Artifact.ContentStatus;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADED;
import static software.wings.beans.artifact.Artifact.ContentStatus.FAILED;
import static software.wings.beans.artifact.Artifact.ContentStatus.METADATA_ONLY;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delay.DelayEventHelper;
import io.harness.delay.DelayEventNotifyData;
import io.harness.delegate.beans.ResponseData;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.service.intfc.ArtifactService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ArtifactCheckState extends State {
  @Inject private transient ArtifactService artifactService;
  @Inject private transient DelayEventHelper delayEventHelper;

  private static int DELAY_TIME_IN_SEC = 60;

  public ArtifactCheckState(String name) {
    super(name, StateType.ARTIFACT_CHECK.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    List<Artifact> artifacts = workflowStandardParams.getArtifacts();
    if (isEmpty(artifacts)) {
      return ExecutionResponse.builder().errorMessage("Artifacts are not required.").build();
    }
    List<Artifact> failedArtifacts =
        artifacts.stream()
            .filter(artifact -> artifact.getStatus() == Status.FAILED || artifact.getStatus() == Status.ERROR)
            .collect(toList());

    if (!isEmpty(failedArtifacts)) {
      return ExecutionResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("One or more artifacts: " + failedArtifacts + " are in failed status")
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
      // TODO: auto downloaded is not done well in artifact stream - temporarily using Constants
      ContentStatus artifactContentStatus = artifactService.getArtifactContentStatus(artifact);
      if (DOWNLOADED == artifactContentStatus || METADATA_ONLY == artifactContentStatus) {
        return;
      }
      // Artifact needs to be downloaded now
      artifactService.startArtifactCollection(artifact.getAccountId(), artifact.getUuid());
      String resumeId = delayEventHelper.delay(DELAY_TIME_IN_SEC, ImmutableMap.of("artifactId", artifact.getUuid()));
      correlationIds.add(resumeId);
      artifactNamesForDownload.add(artifact.getDisplayName());
    });

    if (artifactNamesForDownload.isEmpty()) {
      return getExecutionResponse(artifacts);
    }

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(correlationIds)
        .errorMessage("Waiting for artifacts:" + artifactNamesForDownload + " to be downloaded")
        .build();
  }

  private ExecutionResponse getExecutionResponse(List<Artifact> artifacts) {
    return ExecutionResponse.builder()
        .errorMessage(
            "All artifacts: " + artifacts.stream().map(Artifact::getDisplayName).collect(toList()) + " are available.")
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    List<String> artifactNamesForDownload = new ArrayList<>();
    List<String> correlationIds = new ArrayList<>();
    List<Artifact> failedArtifacts = new ArrayList<>();

    logger.info("Received handleAsyncResponse - response: {}", response);
    response.values().forEach(notifyResponseData -> {
      String artifactId = "";
      if (notifyResponseData instanceof DelayEventNotifyData) {
        DelayEventNotifyData delayEventNotifyData = (DelayEventNotifyData) notifyResponseData;
        artifactId = delayEventNotifyData.getContext().get("artifactId");
      }

      Artifact artifact = artifactService.get(artifactId);
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
      return ExecutionResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("One or more artifacts: "
              + failedArtifacts.stream().map(Artifact::getDisplayName).collect(toList()) + " are in failed status")
          .build();
    }

    if (artifactNamesForDownload.isEmpty()) {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      List<Artifact> artifacts = Preconditions.checkNotNull(workflowStandardParams.getArtifacts());
      return getExecutionResponse(artifacts);
    }

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(correlationIds)
        .errorMessage("Waiting for artifacts:" + artifactNamesForDownload + " to be downloaded")
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // TODO :  Abort the delay event
  }
}
