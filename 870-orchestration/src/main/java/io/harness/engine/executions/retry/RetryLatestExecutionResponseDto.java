package io.harness.engine.executions.retry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@Schema(name = "RetryLatestExecutionResponse",
    description = "This is the view of having the Execution id of the Latest Execution of all retired Executions")
public class RetryLatestExecutionResponseDto {
  String errorMessage;
  String latestExecutionId;
}
