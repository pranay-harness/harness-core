package software.wings.exception;

import static io.harness.eraro.ErrorCode.BASELINE_CONFIGURATION_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class InvalidBaselineConfigurationException extends WingsException {
  public InvalidBaselineConfigurationException(String message) {
    super(message, null, BASELINE_CONFIGURATION_ERROR, Level.ERROR, null, null);
  }
}
