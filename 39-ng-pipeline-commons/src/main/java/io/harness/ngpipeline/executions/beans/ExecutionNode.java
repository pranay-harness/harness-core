package io.harness.ngpipeline.executions.beans;

import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;
import io.harness.state.io.FailureInfo;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionNode {
  String uuid;
  String name;
  Long startTs;
  Long endTs;
  String stepType;
  ExecutionStatus status;
  FailureInfo failureInfo;
}
