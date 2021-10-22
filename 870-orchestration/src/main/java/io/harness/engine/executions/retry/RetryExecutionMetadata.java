package io.harness.engine.executions.retry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
public class RetryExecutionMetadata {
  String parentExecutionId;
  String rootExecutionId;
}
