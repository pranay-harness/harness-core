package io.harness.exception;

public class CyclicBarrierException extends RuntimeException {
  public CyclicBarrierException(Exception cause) {
    super(cause);
  }
}
