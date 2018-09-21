package io.harness.distribution.barrier;

public class UnableToLoadBarrierException extends Exception {
  public UnableToLoadBarrierException(String message) {
    super(message);
  }

  public UnableToLoadBarrierException(Exception cause) {
    super(cause);
  }
}
