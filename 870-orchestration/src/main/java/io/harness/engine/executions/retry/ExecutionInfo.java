package io.harness.engine.executions.retry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.ExecutionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@Schema(name = "ExecutionInfo", description = "This is the view for a particular Execution in Retry History")
public class ExecutionInfo {
  String uuid;
  Long startTs;
  Long endTs;
  ExecutionStatus status;
}
