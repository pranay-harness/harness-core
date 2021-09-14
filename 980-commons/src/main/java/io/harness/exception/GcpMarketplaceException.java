/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.GCP_MARKETPLACE_EXCEPTION;

import io.harness.eraro.Level;

public class GcpMarketplaceException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public GcpMarketplaceException(String message) {
    super(message, null, GCP_MARKETPLACE_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public GcpMarketplaceException(String message, Throwable cause) {
    super(message, cause, GCP_MARKETPLACE_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
