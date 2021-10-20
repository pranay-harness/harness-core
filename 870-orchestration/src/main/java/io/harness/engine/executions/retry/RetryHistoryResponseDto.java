package io.harness.engine.executions.retry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
public class RetryHistoryResponseDto {
  String errorMessage;
  String latestExecutionId;
  List<ExecutionInfo> executionInfos;
}
