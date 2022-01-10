/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class DuplicateStateNameException extends WingsException {
  private static final String DUPLICATE_STATE_KEY = "dupStateNames";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public DuplicateStateNameException(String details) {
    super(null, null, ErrorCode.DUPLICATE_STATE_NAMES, Level.ERROR, null, null);
    super.param(DUPLICATE_STATE_KEY, details);
  }
}
