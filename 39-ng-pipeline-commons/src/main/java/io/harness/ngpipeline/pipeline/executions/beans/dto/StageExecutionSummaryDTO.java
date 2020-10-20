package io.harness.ngpipeline.pipeline.executions.beans.dto;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CDStageExecutionSummaryDTO.class, name = "stage")
  , @JsonSubTypes.Type(value = ParallelStageExecutionSummaryDTO.class, name = "parallel")
})
public interface StageExecutionSummaryDTO {}
