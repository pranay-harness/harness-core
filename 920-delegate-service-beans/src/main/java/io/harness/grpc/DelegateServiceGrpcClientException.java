/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc;

public class DelegateServiceGrpcClientException extends RuntimeException {
  public DelegateServiceGrpcClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public DelegateServiceGrpcClientException(String message) {
    super(message);
  }
}
