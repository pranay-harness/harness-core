package io.harness.ngtriggers.dtos;

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
public class NGPipelineExecutionResponseDTO {
  PlanExecution planExecution;
  boolean isErrorResponse;
  NGPipelineErrorWrapperDTO pipelineErrorResponse;
}
