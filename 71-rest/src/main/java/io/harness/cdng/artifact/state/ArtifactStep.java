package io.harness.cdng.artifact.state;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskBuilder;
import io.harness.cdng.artifact.bean.Artifact;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.yaml.ArtifactConfig;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskResponse;
import io.harness.cdng.artifact.service.ArtifactService;
import io.harness.cdng.artifact.service.ArtifactSourceService;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataBuilder;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepTransput;
import io.harness.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Produces(Step.class)
@Slf4j
public class ArtifactStep implements Step, TaskExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("ARTIFACT_STEP").build();
  @Inject private ArtifactSourceService artifactSourceService;
  @Inject private ArtifactService artifactService;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  // Default timeout of 1 minute.
  private static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

  @Override
  public Task obtainTask(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    ArtifactStepParameters parameters = (ArtifactStepParameters) stepParameters;
    ArtifactSource artifactSource = getArtifactSource(parameters, (String) ambiance.getInputArgs().get("accountId"));

    String waitId = generateUuid();
    ArtifactTaskParameters taskParameters = ArtifactUtils.getArtifactTaskParameters(artifactSource);
    final TaskDataBuilder dataBuilder = TaskData.builder().async(true).taskType(TaskType.ARTIFACT_COLLECT_TASK.name());
    DelegateTaskBuilder delegateTaskBuilder =
        DelegateTask.builder().appId(artifactSource.getAccountId()).waitId(waitId);

    // Set timeout.
    dataBuilder.parameters(new Object[] {taskParameters}).timeout(DEFAULT_TIMEOUT);
    delegateTaskBuilder.accountId(artifactSource.getAccountId());
    delegateTaskBuilder.data(dataBuilder.build());
    return delegateTaskBuilder.build();
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    ResponseData notifyResponseData = responseDataMap.values().iterator().next();

    if (notifyResponseData instanceof ArtifactTaskResponse) {
      ArtifactTaskResponse taskResponse = (ArtifactTaskResponse) notifyResponseData;
      switch (taskResponse.getCommandExecutionStatus()) {
        case SUCCESS:
          stepResponseBuilder.status(Status.SUCCEEDED);
          break;
        case FAILURE:
          stepResponseBuilder.status(Status.FAILED);
          stepResponseBuilder.failureInfo(FailureInfo.builder().errorMessage(taskResponse.getErrorMessage()).build());
          break;
        default:
          throw new ArtifactServerException(
              "Unhandled type CommandExecutionStatus: " + taskResponse.getCommandExecutionStatus().name());
      }

      ArtifactSource artifactSource =
          getArtifactSource((ArtifactStepParameters) stepParameters, (String) ambiance.getInputArgs().get("accountId"));
      Artifact artifact = taskResponse.getArtifactAttributes().getArtifact(
          artifactSource.getAccountId(), artifactSource.getUuid(), artifactSource.getSourceType());
      Artifact savedArtifact = artifactService.create(artifact);
      executionSweepingOutputService.consumeInternal(ambiance, "artifact", savedArtifact, 0);
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

  private ArtifactSource getArtifactSource(ArtifactStepParameters parameters, String accountId) {
    ArtifactConfig primary = parameters.getArtifactListConfig().getPrimary();
    ArtifactSource artifactSource = ArtifactSource.builder()
                                        .accountId(accountId)
                                        .sourceType(primary.getSourceType())
                                        .sourceAttributes(primary.getSpec().getSourceAttributes())
                                        .uniqueHash(primary.getSpec().getUniqueHash())
                                        .build();
    return artifactSourceService.saveOrGetArtifactStream(artifactSource);
  }
}
