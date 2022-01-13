/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

public class EmptyRestResponseException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public EmptyRestResponseException(@NotEmpty String uri, String message) {
    super(null, null, ErrorCode.UNKNOWN_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_KEY,
        HarnessStringUtils.join(StringUtils.SPACE,
            "Received empty rest response for"
                + " call to",
            uri, message));
  }
}
