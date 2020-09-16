package io.harness.cdng.pipeline.mappers;

import io.harness.cdng.pipeline.executions.beans.CDStageExecution;
import io.harness.cdng.pipeline.executions.beans.ParallelStageExecution;
import io.harness.cdng.pipeline.executions.beans.PipelineExecution;
import io.harness.cdng.pipeline.executions.beans.StageExecution;
import io.harness.cdng.pipeline.executions.beans.dto.CDStageExecutionDTO;
import io.harness.cdng.pipeline.executions.beans.dto.ParallelStageExecutionDTO;
import io.harness.cdng.pipeline.executions.beans.dto.PipelineExecutionDTO;
import io.harness.cdng.pipeline.executions.beans.dto.StageExecutionDTO;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class ExecutionToDtoMapper {
  public PipelineExecutionDTO writeExecutionDto(PipelineExecution pipelineExecution) {
    List<StageExecutionDTO> stageExecutionDTOs = pipelineExecution.getStageExecutionSummaryElements()
                                                     .stream()
                                                     .map(ExecutionToDtoMapper::writeStageExecutionDto)
                                                     .collect(Collectors.toList());
    return PipelineExecutionDTO.builder()
        .endedAt(pipelineExecution.getEndedAt())
        .envIdentifiers(pipelineExecution.getEnvIdentifiers())
        .pipelineIdentifier(pipelineExecution.getPipelineIdentifier())
        .pipelineName(pipelineExecution.getPipelineName())
        .planExecutionId(pipelineExecution.getPlanExecutionId())
        .serviceDefinitionTypes(pipelineExecution.getServiceDefinitionTypes())
        .serviceIdentifiers(pipelineExecution.getServiceIdentifiers())
        .stageExecutionSummaryElements(stageExecutionDTOs)
        .stageIdentifiers(pipelineExecution.getStageIdentifiers())
        .stageTypes(pipelineExecution.getStageTypes())
        .startedAt(pipelineExecution.getStartedAt())
        .pipelineExecutionStatus(pipelineExecution.getPipelineExecutionStatus())
        .tags(pipelineExecution.getTags())
        .triggeredBy(pipelineExecution.getTriggeredBy())
        .triggerType(pipelineExecution.getTriggerType())
        .build();
  }

  public StageExecutionDTO writeStageExecutionDto(StageExecution stageExecution) {
    if (stageExecution instanceof ParallelStageExecution) {
      return writeParallelStageExecutionDto((ParallelStageExecution) stageExecution);
    }
    return writeCDStageExecutionDto((CDStageExecution) stageExecution);
  }

  private StageExecutionDTO writeParallelStageExecutionDto(ParallelStageExecution parallelStageExecution) {
    return ParallelStageExecutionDTO.builder()
        .stageExecutions(parallelStageExecution.getStageExecutions()
                             .stream()
                             .map(ExecutionToDtoMapper::writeStageExecutionDto)
                             .collect(Collectors.toList()))
        .build();
  }

  private StageExecutionDTO writeCDStageExecutionDto(CDStageExecution cdStageExecution) {
    return CDStageExecutionDTO.builder()
        .artifactsDeployed(cdStageExecution.getArtifactsDeployed())
        .deploymentType(cdStageExecution.getDeploymentType())
        .endedAt(cdStageExecution.getEndedAt())
        .envIdentifier(cdStageExecution.getEnvIdentifier())
        .errorMsg(cdStageExecution.getErrorMsg())
        .planExecutionId(cdStageExecution.getPlanExecutionId())
        .serviceIdentifier(cdStageExecution.getServiceIdentifier())
        .stageIdentifier(cdStageExecution.getStageIdentifier())
        .stageName(cdStageExecution.getStageName())
        .startedAt(cdStageExecution.getStartedAt())
        .pipelineExecutionStatus(cdStageExecution.getPipelineExecutionStatus())
        .build();
  }
}
