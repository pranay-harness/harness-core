package io.harness.ngpipeline.pipeline.beans.resources;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.execution.PlanExecution;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("NGPipelineExecutionResponse")
@ToBeDeleted
@Deprecated
public class NGPipelineExecutionResponseDTO {
  PlanExecution planExecution;
  boolean isErrorResponse;
  NGPipelineErrorWrapperDTO pipelineErrorResponse;
}
