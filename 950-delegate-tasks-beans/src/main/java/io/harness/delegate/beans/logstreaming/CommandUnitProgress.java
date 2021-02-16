package io.harness.delegate.beans.logstreaming;

import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandUnitProgress {
  CommandExecutionStatus status;
  long startTime;
  long endTime;
}
