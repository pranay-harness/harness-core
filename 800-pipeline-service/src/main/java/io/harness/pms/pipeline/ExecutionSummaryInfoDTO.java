package io.harness.pms.pipeline;

import io.harness.pms.execution.ExecutionStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("executionSummaryInfo")
public class ExecutionSummaryInfoDTO {
  int numOfErrors; // total number of errors in the last ten days
  List<Integer> deployments; // no of deployments for each of the last 10 days, most recent first
  Long lastExecutionTs;
  ExecutionStatus lastExecutionStatus;
}
