package io.harness.engine.executions.retry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@Schema(name = "RetryExecutionMetadata",
    description = "This gives the Parent and Root execution id of the Execution part of Retried Execution")
public class RetryExecutionMetadata {
  String parentExecutionId;
  String rootExecutionId;
}
