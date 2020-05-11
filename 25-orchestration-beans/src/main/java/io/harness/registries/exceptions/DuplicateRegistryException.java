package io.harness.registries.exceptions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.registries.RegistryType;

@OwnedBy(CDC)
public class DuplicateRegistryException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public DuplicateRegistryException(RegistryType registryType, String message) {
    super(null, null, ErrorCode.ENGINE_REGISTRY_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, HarnessStringUtils.join("", "[RegistryType: ", registryType.toString(), "]", message));
  }
}
