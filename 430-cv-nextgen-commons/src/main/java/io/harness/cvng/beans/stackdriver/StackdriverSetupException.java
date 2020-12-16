package io.harness.cvng.beans.stackdriver;

public class StackdriverSetupException extends RuntimeException {
  public StackdriverSetupException(String errMsg) {
    super(errMsg);
  }

  public StackdriverSetupException(Throwable throwable) {
    super(throwable);
  }
}
