/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.exception;

public class CVWebhookException extends RuntimeException {
  public CVWebhookException(Exception e) {
    super(e);
  }

  public CVWebhookException(String message) {
    super(message);
  }

  public CVWebhookException(String message, Exception e) {
    super(message, e);
  }
}
