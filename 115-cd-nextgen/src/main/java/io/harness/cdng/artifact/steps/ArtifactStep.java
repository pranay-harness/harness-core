package io.harness.cdng.artifact.steps;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskResponse;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.common.AmbianceHelper;
import io.harness.cdng.orchestration.StepUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.facilitator.modes.taskv2.TaskV2Executable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ArtifactStep
    implements Step, TaskExecutable<ArtifactStepParameters>, TaskV2Executable<ArtifactStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type("ARTIFACT_STEP").build();

  // Default timeout of 1 minute.
  private static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

  @Override
  public Task obtainTask(Ambiance ambiance, ArtifactStepParameters stepParameters, StepInputPackage inputPackage) {
    logger.info("Executing deployment stage with params [{}]", stepParameters);
    ArtifactConfig finalArtifact = applyArtifactsOverlay(stepParameters);
    ArtifactSource artifactSource = getArtifactSource(finalArtifact, AmbianceHelper.getAccountId(ambiance));

    final ArtifactTaskParameters taskParameters =
        ArtifactUtils.getArtifactTaskParameters(artifactSource.getAccountId(), finalArtifact.getSourceAttributes());
    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .taskType(TaskType.ARTIFACT_COLLECT_TASK.name())
                                  .parameters(new Object[] {taskParameters})
                                  .timeout(DEFAULT_TIMEOUT)
                                  .build();

    return StepUtils.prepareDelegateTaskInput(artifactSource.getAccountId(), taskData,
        ImmutableMap.of(Cd1SetupFields.APP_ID_FIELD, artifactSource.getAccountId()));
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, ArtifactStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    ResponseData notifyResponseData = responseDataMap.values().iterator().next();

    if (notifyResponseData instanceof ArtifactTaskResponse) {
      ArtifactTaskResponse taskResponse = (ArtifactTaskResponse) notifyResponseData;
      switch (taskResponse.getCommandExecutionStatus()) {
        case SUCCESS:
          stepResponseBuilder.status(Status.SUCCEEDED);
          stepResponseBuilder.stepOutcome(getStepOutcome(taskResponse, stepParameters));
          break;
        case FAILURE:
          stepResponseBuilder.status(Status.FAILED);
          stepResponseBuilder.failureInfo(FailureInfo.builder().errorMessage(taskResponse.getErrorMessage()).build());
          break;
        default:
          throw new ArtifactServerException(
              "Unhandled type CommandExecutionStatus: " + taskResponse.getCommandExecutionStatus().name());
      }
    } else if (notifyResponseData instanceof ErrorNotifyResponseData) {
      stepResponseBuilder.status(Status.FAILED);
      stepResponseBuilder.failureInfo(
          FailureInfo.builder().errorMessage(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage()).build());
      return stepResponseBuilder.build();
    } else {
      logger.error("Unhandled ResponseData class " + notifyResponseData.getClass().getCanonicalName());
    }

    return stepResponseBuilder.build();
  }

  @VisibleForTesting
  ArtifactSource getArtifactSource(ArtifactConfig artifactConfig, String accountId) {
    return artifactConfig.getArtifactSource(accountId);
  }

  @VisibleForTesting
  StepOutcome getStepOutcome(ArtifactTaskResponse taskResponse, ArtifactStepParameters stepParameters) {
    ArtifactOutcome artifact =
        applyArtifactsOverlay(stepParameters).getArtifactOutcome(taskResponse.getArtifactAttributes());
    String outcomeKey = artifact.getArtifactType() + ":" + artifact.getIdentifier();
    return StepOutcome.builder().name(outcomeKey).outcome(artifact).build();
  }

  @VisibleForTesting
  ArtifactConfig applyArtifactsOverlay(ArtifactStepParameters stepParameters) {
    List<ArtifactConfig> artifactList = new LinkedList<>();
    if (stepParameters.getArtifact() != null) {
      artifactList.add(stepParameters.getArtifact());
    }
    if (stepParameters.getArtifactOverrideSet() != null) {
      artifactList.add(stepParameters.getArtifactOverrideSet());
    }
    if (stepParameters.getArtifactStageOverride() != null) {
      artifactList.add(stepParameters.getArtifactStageOverride());
    }
    if (EmptyPredicate.isEmpty(artifactList)) {
      throw new InvalidArgumentsException("No Artifact details defined.");
    }
    ArtifactConfig resultantArtifact = artifactList.get(0);
    for (ArtifactConfig artifact : artifactList) {
      resultantArtifact = resultantArtifact.applyOverrides(artifact);
    }
    return resultantArtifact;
  }
}
