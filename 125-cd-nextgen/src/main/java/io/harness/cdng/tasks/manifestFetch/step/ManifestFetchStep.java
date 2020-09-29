package io.harness.cdng.tasks.manifestFetch.step;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Collections.EMPTY_LIST;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.ExecutionStatus;
import io.harness.cdng.orchestration.StepUtils;
import io.harness.cdng.pipeline.steps.NGStepTypes;
import io.harness.cdng.tasks.manifestFetch.ManifestFetchHelper;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchOutcome.ManifestFetchOutcomeBuilder;
import io.harness.common.AmbianceHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataBuilder;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ManifestFetchStep implements Step, TaskExecutable<ManifestFetchParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type(NGStepTypes.MANIFEST_FETCH).build();
  @Inject ManifestFetchHelper manifestFetchHelper;

  // Default timeout of 1 minute.
  private static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

  @Override
  public Task obtainTask(Ambiance ambiance, ManifestFetchParameters stepParameters, StepInputPackage inputPackage) {
    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        manifestFetchHelper.generateFetchFilesConfigForManifests(stepParameters);
    final String activityId = generateUuid();
    final String accountId = AmbianceHelper.getAccountId(ambiance);
    GitFetchRequest gitFetchRequest = GitFetchRequest.builder()
                                          .activityId(activityId)
                                          .accountId(accountId)
                                          .gitFetchFilesConfigs(gitFetchFilesConfigs)
                                          .build();

    final TaskDataBuilder dataBuilder = TaskData.builder()
                                            .async(true)
                                            .taskType(TaskType.GIT_FETCH_NEXT_GEN_TASK.name())
                                            .parameters(new Object[] {gitFetchRequest})
                                            .timeout(DEFAULT_TIMEOUT);

    return StepUtils.prepareDelegateTaskInput(accountId, dataBuilder.build(), ambiance.getSetupAbstractions());
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, ManifestFetchParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    GitCommandExecutionResponse executionResponse =
        (GitCommandExecutionResponse) responseDataMap.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getGitCommandStatus() == GitCommandExecutionResponse.GitCommandStatus.SUCCESS ? SUCCESS
                                                                                                        : FAILED;

    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    ManifestFetchOutcomeBuilder manifestFetchOutcomeBuilder = ManifestFetchOutcome.builder();
    if (executionStatus == SUCCESS) {
      GitFetchFilesFromMultipleRepoResult gitCommandResult =
          (GitFetchFilesFromMultipleRepoResult) executionResponse.getGitCommandResult();

      Map<String, GitFetchFilesResult> filesFromMultipleRepo = gitCommandResult.getFilesFromMultipleRepo();

      if (isNotEmpty(stepParameters.getServiceSpecManifestAttributes())) {
        List<ManifestFetchOutcome.ManifestDataDetails> manifestDataDetailsForSpec =
            stepParameters.getServiceSpecManifestAttributes()
                .stream()
                .map(manifestAttributes
                    -> ManifestFetchOutcome.ManifestDataDetails.builder()
                           .identifier(manifestAttributes.getIdentifier())
                           .gitFiles(filesFromMultipleRepo.get(manifestAttributes.getIdentifier()).getFiles())
                           .build())
                .collect(toList());

        manifestFetchOutcomeBuilder.manifestDataDetailsForSpec(manifestDataDetailsForSpec);
      }

      if (isNotEmpty(stepParameters.getOverridesManifestAttributes())) {
        List<ManifestFetchOutcome.ManifestDataDetails> manifestDataDetailsForOverrides =
            stepParameters.getOverridesManifestAttributes()
                .stream()
                .map(manifestAttributes
                    -> ManifestFetchOutcome.ManifestDataDetails.builder()
                           .identifier(manifestAttributes.getIdentifier())
                           .gitFiles(filesFromMultipleRepo.containsKey(manifestAttributes.getIdentifier())
                                   ? filesFromMultipleRepo.get(manifestAttributes.getIdentifier()).getFiles()
                                   : EMPTY_LIST)
                           .build())
                .collect(toList());

        manifestFetchOutcomeBuilder.manifestDataDetailsForOverrides(manifestDataDetailsForOverrides);
        stepResponseBuilder.status(Status.SUCCEEDED);
      }

      stepResponseBuilder.stepOutcome(
          StepOutcome.builder().outcome(manifestFetchOutcomeBuilder.build()).name("manifestData").build());
    } else {
      stepResponseBuilder.status(Status.ERRORED);
      stepResponseBuilder.failureInfo(FailureInfo.builder().errorMessage(executionResponse.getErrorMessage()).build());
    }

    return stepResponseBuilder.build();
  }
}
