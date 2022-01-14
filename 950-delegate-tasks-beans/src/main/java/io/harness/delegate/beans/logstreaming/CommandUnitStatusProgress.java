package io.harness.delegate.beans.logstreaming;

import io.harness.delegate.beans.DelegateProgressData;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandUnitStatusProgress implements DelegateProgressData {
  String commandUnitName;
  CommandExecutionStatus commandExecutionStatus;
}
