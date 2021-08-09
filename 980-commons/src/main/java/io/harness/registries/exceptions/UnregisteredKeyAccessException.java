package io.harness.registries.exceptions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(CDC)
public class UnregisteredKeyAccessException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public UnregisteredKeyAccessException(String registryType, String message) {
    super(message, null, ErrorCode.REGISTRY_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, HarnessStringUtils.join("", "[RegistryType: ", registryType, "]", message));
  }
}
