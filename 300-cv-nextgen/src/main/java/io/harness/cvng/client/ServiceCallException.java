/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.client;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceCallException extends RuntimeException {
  private int code;
  private String errorMessage;
  private String errorBody;
  public ServiceCallException(int code, String message, Throwable cause) {
    super("Response code: " + code + ", Message: " + message, cause);
    this.code = code;
    this.errorMessage = message;
  }

  public ServiceCallException(int code, String message, String errorBody) {
    super("Response code: " + code + ", Message: " + message + ", Error: " + errorBody);
    this.code = code;
    this.errorMessage = message;
    this.errorBody = errorBody;
  }
  public ServiceCallException(Throwable throwable) {
    super(throwable);
  }
}
