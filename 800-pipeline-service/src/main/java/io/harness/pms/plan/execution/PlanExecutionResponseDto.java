package io.harness.pms.plan.execution;

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
@ApiModel("PlanExecutionResponseDto")
public class PlanExecutionResponseDto {
  PlanExecution planExecution;
}
