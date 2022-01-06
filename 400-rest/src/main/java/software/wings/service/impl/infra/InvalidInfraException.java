/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
