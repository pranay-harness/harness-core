package io.harness.pms.pipeline.mappers;

import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.pipeline.ExecutionTriggerInfo;
import io.harness.pms.pipeline.entity.PipelineExecutionSummaryEntity;
import io.harness.pms.pipeline.resource.PipelineExecutionSummaryDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PipelineExecutionSummaryDtoMapper {
  public PipelineExecutionSummaryDTO toDto(PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    return PipelineExecutionSummaryDTO.builder()
        .name(pipelineExecutionSummaryEntity.getName())
        .createdAt(pipelineExecutionSummaryEntity.getCreatedAt())
        .layoutNodeMap(pipelineExecutionSummaryEntity.getLayoutNodeMap())
        .moduleInfo(pipelineExecutionSummaryEntity.getModuleInfo())
        .startingNodeId(pipelineExecutionSummaryEntity.getStartingNodeId())
        .planExecutionId(pipelineExecutionSummaryEntity.getPlanExecutionId())
        .startTs(pipelineExecutionSummaryEntity.getStartTs())
        .endTs(pipelineExecutionSummaryEntity.getEndTs())
        .status(pipelineExecutionSummaryEntity.getStatus())
        .executionTriggerInfo(ExecutionTriggerInfo.builder().build())
        .build();
  }
}
