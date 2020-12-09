package io.harness.pms.execution.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionErrorInfo {
  String message;
}
