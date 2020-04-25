package io.harness.registries;

import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class DuplicateRegistryException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public DuplicateRegistryException(RegistryType registryType, String message) {
    super(null, null, ErrorCode.UNKNOWN_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_KEY,
        HarnessStringUtils.join(SPACE, "Error while registering for ", registryType.toString(), " Registry", message));
  }
}
