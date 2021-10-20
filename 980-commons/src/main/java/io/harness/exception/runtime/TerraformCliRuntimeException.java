package io.harness.exception.runtime;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@OwnedBy(CDP)
@EqualsAndHashCode(callSuper = false)
public class TerraformCliRuntimeException extends RuntimeException {
  String command;
  String cliError;

  public TerraformCliRuntimeException(String message, String command, String cliError) {
    super(message);
    this.command = command;
    this.cliError = cliError;
  }
}
