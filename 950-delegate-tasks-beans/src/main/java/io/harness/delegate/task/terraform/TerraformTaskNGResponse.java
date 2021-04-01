package io.harness.delegate.task.terraform;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
public class TerraformTaskNGResponse implements DelegateResponseData {
  CommandExecutionStatus commandExecutionStatus;
  String errorMessage;
  @NonFinal @Setter UnitProgressData unitProgressData;
}
