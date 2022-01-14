package io.harness.secret;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public class ConfigSecretException extends RuntimeException {
  public ConfigSecretException(String message) {
    super(message);
  }
}
