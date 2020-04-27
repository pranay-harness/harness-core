package io.harness.registries.exceptions;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.registries.RegistryType;

public class UnregisteredKeyAccessException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public UnregisteredKeyAccessException(RegistryType registryType, String message) {
    super(null, null, ErrorCode.ENGINE_REGISTRY_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, HarnessStringUtils.join("", "[RegistryType: ", registryType.toString(), "]", message));
  }
}
