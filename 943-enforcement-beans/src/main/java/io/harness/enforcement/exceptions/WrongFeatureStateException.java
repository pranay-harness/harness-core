package io.harness.enforcement.exceptions;

public class WrongFeatureStateException extends Exception {
  private boolean isInvalidLicense;

  public WrongFeatureStateException(String message, Throwable cause, boolean isInvalidLicense) {
    super(message, cause);
    this.isInvalidLicense = isInvalidLicense;
  }

  public boolean isInvalidLicense() {
    return isInvalidLicense;
  }
}
