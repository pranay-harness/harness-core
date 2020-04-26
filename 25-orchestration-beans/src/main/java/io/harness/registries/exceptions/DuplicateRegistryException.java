package io.harness.registries.exceptions;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.registries.RegistryType;

public class DuplicateRegistryException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public DuplicateRegistryException(RegistryType registryType, String message) {
    super(HarnessStringUtils.join("[RegistryType: ", registryType.toString(), "]", message), null,
        ErrorCode.ENGINE_REGISTRY_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, HarnessStringUtils.join("[RegistryType: ", registryType.toString(), "]", message));
  }
}
