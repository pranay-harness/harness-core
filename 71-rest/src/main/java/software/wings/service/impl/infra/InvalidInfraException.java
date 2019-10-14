package software.wings.service.impl.infra;

import static io.harness.eraro.ErrorCode.INVALID_INFRA_CONFIGURATION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class InvalidInfraException extends WingsException {
  public InvalidInfraException(String message) {
    super(message, null, INVALID_INFRA_CONFIGURATION, Level.ERROR, null, null);
  }

  public InvalidInfraException(String message, Throwable cause) {
    super(message, cause, INVALID_INFRA_CONFIGURATION, Level.ERROR, null, null);
  }
}
