package io.harness.grpc;

public class DelegateServiceGrpcClientException extends RuntimeException {
  public DelegateServiceGrpcClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public DelegateServiceGrpcClientException(String message) {
    super(message);
  }
}
