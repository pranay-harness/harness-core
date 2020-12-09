package io.harness.pms.pipeline.resource;

import io.harness.pms.execution.beans.ExecutionGraph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("PipelineExecutionDetail")
public class PipelineExecutionDetailDTO {
  PipelineExecutionSummaryDTO pipelineExecutionSummary;
  ExecutionGraph executionGraph;
}
