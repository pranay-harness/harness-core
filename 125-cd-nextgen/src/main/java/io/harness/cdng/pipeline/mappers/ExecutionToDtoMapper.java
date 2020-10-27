package io.harness.cdng.pipeline.mappers;

import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.ParallelStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.StageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.dto.CDStageExecutionSummaryDTO;
import io.harness.ngpipeline.pipeline.executions.beans.dto.ParallelStageExecutionSummaryDTO;
import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.ngpipeline.pipeline.executions.beans.dto.StageExecutionSummaryDTO;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class ExecutionToDtoMapper {
  public PipelineExecutionSummaryDTO writeExecutionDto(PipelineExecutionSummary pipelineExecutionSummary) {
    List<StageExecutionSummaryDTO> stageExecutionSummaryDTOS =
        pipelineExecutionSummary.getStageExecutionSummarySummaryElements()
            .stream()
            .map(ExecutionToDtoMapper::writeStageExecutionDto)
            .collect(Collectors.toList());
    return PipelineExecutionSummaryDTO.builder()
        .endedAt(pipelineExecutionSummary.getEndedAt())
        .envIdentifiers(pipelineExecutionSummary.getEnvIdentifiers())
        .pipelineIdentifier(pipelineExecutionSummary.getPipelineIdentifier())
        .pipelineName(pipelineExecutionSummary.getPipelineName())
        .planExecutionId(pipelineExecutionSummary.getPlanExecutionId())
        .serviceDefinitionTypes(pipelineExecutionSummary.getServiceDefinitionTypes())
        .serviceIdentifiers(pipelineExecutionSummary.getServiceIdentifiers())
        .stageExecutionSummaryElements(stageExecutionSummaryDTOS)
        .stageIdentifiers(pipelineExecutionSummary.getStageIdentifiers())
        .stageTypes(pipelineExecutionSummary.getStageTypes())
        .startedAt(pipelineExecutionSummary.getStartedAt())
        .executionStatus(pipelineExecutionSummary.getExecutionStatus())
        .tags(TagMapper.convertToMap(pipelineExecutionSummary.getTags()))
        .deploymentId("DeploymentIdPlaceHolder")
        .triggerInfo(pipelineExecutionSummary.getTriggerInfo())
        .failedStagesCount(getCountForGivenStatus(
            pipelineExecutionSummary.getStageExecutionSummarySummaryElements(), ExecutionStatus.FAILED))
        .successfulStagesCount(getCountForGivenStatus(
            pipelineExecutionSummary.getStageExecutionSummarySummaryElements(), ExecutionStatus.SUCCESS))
        .runningStagesCount(getCountForGivenStatus(
            pipelineExecutionSummary.getStageExecutionSummarySummaryElements(), ExecutionStatus.RUNNING))
        .errorInfo(pipelineExecutionSummary.getErrorInfo())
        .build();
  }

  public StageExecutionSummaryDTO writeStageExecutionDto(StageExecutionSummary stageExecutionSummary) {
    if (stageExecutionSummary instanceof ParallelStageExecutionSummary) {
      return writeParallelStageExecutionDto((ParallelStageExecutionSummary) stageExecutionSummary);
    }
    return writeCDStageExecutionDto((CDStageExecutionSummary) stageExecutionSummary);
  }

  private StageExecutionSummaryDTO writeParallelStageExecutionDto(
      ParallelStageExecutionSummary parallelStageExecution) {
    return ParallelStageExecutionSummaryDTO.builder()
        .stageExecutions(parallelStageExecution.getStageExecutionSummaries()
                             .stream()
                             .map(ExecutionToDtoMapper::writeStageExecutionDto)
                             .collect(Collectors.toList()))
        .build();
  }

  private StageExecutionSummaryDTO writeCDStageExecutionDto(CDStageExecutionSummary cdStageExecutionSummary) {
    return CDStageExecutionSummaryDTO.builder()
        .serviceDefinitionType(cdStageExecutionSummary.getServiceDefinitionType())
        .endedAt(cdStageExecutionSummary.getEndedAt())
        .envIdentifier(cdStageExecutionSummary.getEnvIdentifier())
        .errorInfo(cdStageExecutionSummary.getErrorInfo())
        .planExecutionId(cdStageExecutionSummary.getPlanExecutionId())
        .serviceIdentifier(cdStageExecutionSummary.getServiceIdentifier())
        .stageIdentifier(cdStageExecutionSummary.getStageIdentifier())
        .stageName(cdStageExecutionSummary.getStageName())
        .startedAt(cdStageExecutionSummary.getStartedAt())
        .executionStatus(cdStageExecutionSummary.getExecutionStatus())
        .serviceInfo(cdStageExecutionSummary.getServiceExecutionSummary())
        .build();
  }

  private long getCountForGivenStatus(
      List<StageExecutionSummary> stageExecutionSummaries, ExecutionStatus requiredStatus) {
    return stageExecutionSummaries.stream()
        .filter(stageExecutionSummary -> !(stageExecutionSummary instanceof ParallelStageExecutionSummary))
        .map(stageExecutionSummary -> (CDStageExecutionSummary) stageExecutionSummary)
        .map(CDStageExecutionSummary::getExecutionStatus)
        .filter(status -> status == requiredStatus)
        .count();
  }
}
