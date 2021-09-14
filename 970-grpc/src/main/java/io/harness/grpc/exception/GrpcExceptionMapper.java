/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.exception;

import io.grpc.Status;

public interface GrpcExceptionMapper<E extends Throwable> {
  Status toStatus(E exception);
  Class getClazz();
}
