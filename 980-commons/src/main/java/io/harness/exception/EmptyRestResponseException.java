/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
