package io.harness.cdng.pipeline.executions.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionErrorInfo {
  String message;
}
