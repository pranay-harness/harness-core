package io.harness.delegate.beans.logstreaming;

import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CommandUnitProgressKeys")
public class CommandUnitProgress {
  CommandExecutionStatus status;
  long startTime;
  long endTime;
}
