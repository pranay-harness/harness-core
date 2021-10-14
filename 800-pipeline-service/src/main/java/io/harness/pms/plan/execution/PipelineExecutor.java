package io.harness.pms.plan.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.retry.RetryExecutionHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.plan.execution.beans.ExecArgs;
import io.harness.pms.plan.execution.beans.dto.RunStageRequestDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class PipelineExecutor {
  ExecutionHelper executionHelper;
  ValidateAndMergeHelper validateAndMergeHelper;
  PlanExecutionMetadataService planExecutionMetadataService;
  RetryExecutionHelper retryExecutionHelper;
  PMSPipelineTemplateHelper pipelineTemplateHelper;

  public PlanExecutionResponseDto runPipelineWithInputSetPipelineYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String moduleType, String runtimeInputYaml, boolean useV2) {
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, null, moduleType,
        runtimeInputYaml, Collections.emptyList(), useV2);
  }

  public PlanExecutionResponseDto runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, List<String> inputSetReferences,
      String pipelineBranch, String pipelineRepoID) {
    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId,
        orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences, pipelineBranch, pipelineRepoID, null);
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, null, moduleType,
        mergedRuntimeInputYaml, Collections.emptyList(), false);
  }

  public PlanExecutionResponseDto runStagesWithRuntimeInputYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String moduleType, RunStageRequestDTO runStageRequestDTO, boolean useV2) {
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, null, moduleType,
        runStageRequestDTO.getRuntimeInputYaml(), runStageRequestDTO.getStageIdentifiers(), useV2);
  }

  public PlanExecutionResponseDto rerunPipelineWithInputSetPipelineYaml(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, String originalExecutionId,
      String runtimeInputYaml, boolean useV2) {
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, originalExecutionId,
        moduleType, runtimeInputYaml, Collections.emptyList(), useV2);
  }

  public PlanExecutionResponseDto rerunPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, String originalExecutionId,
      List<String> inputSetReferences, String pipelineBranch, String pipelineRepoID) {
    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId,
        orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences, pipelineBranch, pipelineRepoID, null);
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, originalExecutionId,
        moduleType, mergedRuntimeInputYaml, Collections.emptyList(), false);
  }

  private PlanExecutionResponseDto startPlanExecution(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String originalExecutionId, String moduleType, String runtimeInputYaml,
      List<String> stagesToRun, boolean useV2) {
    PipelineEntity pipelineEntity =
        executionHelper.fetchPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    ExecutionTriggerInfo triggerInfo = executionHelper.buildTriggerInfo(originalExecutionId);
    ExecArgs execArgs = executionHelper.buildExecutionArgs(pipelineEntity, moduleType, runtimeInputYaml, stagesToRun,
        triggerInfo, originalExecutionId, false, null, null, null);
    PlanExecution planExecution;
    if (useV2) {
      planExecution = executionHelper.startExecutionV2(accountId, orgIdentifier, projectIdentifier,
          execArgs.getMetadata(), execArgs.getPlanExecutionMetadata(), false, null, null);
    } else {
      planExecution = executionHelper.startExecution(accountId, orgIdentifier, projectIdentifier,
          execArgs.getMetadata(), execArgs.getPlanExecutionMetadata(), false, null, null);
    }
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity))
        .build();
  }

  public PlanExecutionResponseDto retryPipelineWithInputSetPipelineYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String moduleType, String inputSetPipelineYaml, String previousExecutionId, List<String> retryStagesIdentifier,
      boolean runAllStages, boolean useV2) {
    PipelineEntity pipelineEntity =
        executionHelper.fetchPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);

    if (!runAllStages && retryStagesIdentifier.size() > 1) {
      // run only failed stage
      retryStagesIdentifier = retryExecutionHelper.fetchOnlyFailedStages(previousExecutionId, retryStagesIdentifier);
    }

    ExecutionTriggerInfo triggerInfo = executionHelper.buildTriggerInfo(null);
    Optional<PlanExecutionMetadata> optionalPlanExecutionMetadata =
        planExecutionMetadataService.findByPlanExecutionId(previousExecutionId);

    if (!optionalPlanExecutionMetadata.isPresent()) {
      throw new InvalidRequestException(String.format("No plan exist for %s planExecutionId", previousExecutionId));
    }
    String previousProcessedYaml = optionalPlanExecutionMetadata.get().getProcessedYaml();
    List<String> identifierOfSkipStages = new ArrayList<>();
    ExecArgs execArgs = executionHelper.buildExecutionArgs(pipelineEntity, moduleType, inputSetPipelineYaml, null,
        triggerInfo, null, true, previousProcessedYaml, retryStagesIdentifier, identifierOfSkipStages);
    PlanExecution planExecution;
    if (useV2) {
      planExecution =
          executionHelper.startExecutionV2(accountId, orgIdentifier, projectIdentifier, execArgs.getMetadata(),
              execArgs.getPlanExecutionMetadata(), true, identifierOfSkipStages, previousExecutionId);
    } else {
      planExecution =
          executionHelper.startExecution(accountId, orgIdentifier, projectIdentifier, execArgs.getMetadata(),
              execArgs.getPlanExecutionMetadata(), true, identifierOfSkipStages, previousExecutionId);
    }
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity))
        .build();
  }
}
