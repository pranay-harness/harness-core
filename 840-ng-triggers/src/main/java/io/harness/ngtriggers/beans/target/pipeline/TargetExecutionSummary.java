package io.harness.ngtriggers.beans.target.pipeline;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TargetExecutionSummary {
  String triggerId;
  String targetId;
  String runtimeInput;
  String planExecutionId;
  String executionStatus;
  Long startTs;
}
