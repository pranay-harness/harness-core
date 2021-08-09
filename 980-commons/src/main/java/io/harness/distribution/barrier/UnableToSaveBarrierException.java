package io.harness.distribution.barrier;

public class UnableToSaveBarrierException extends Exception {
  public UnableToSaveBarrierException(String message) {
    super(message);
  }

  public UnableToSaveBarrierException(Exception cause) {
    super(cause);
  }
}
