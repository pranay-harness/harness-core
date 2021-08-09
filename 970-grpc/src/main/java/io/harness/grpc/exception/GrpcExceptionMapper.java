package io.harness.grpc.exception;

import io.grpc.Status;

public interface GrpcExceptionMapper<E extends Throwable> {
  Status toStatus(E exception);
  Class getClazz();
}
