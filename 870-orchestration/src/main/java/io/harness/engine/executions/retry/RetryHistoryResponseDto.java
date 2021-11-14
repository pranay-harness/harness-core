package io.harness.engine.executions.retry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@Schema(name = "RetryHistoryResponse", description = "This is the view of the history of Retry Failed Pipelines.")
public class RetryHistoryResponseDto {
  String errorMessage;
  String latestExecutionId;
  List<ExecutionInfo> executionInfos;
}
