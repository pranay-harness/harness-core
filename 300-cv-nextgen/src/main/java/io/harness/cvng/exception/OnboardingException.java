package io.harness.cvng.exception;

public class OnboardingException extends RuntimeException {
  public OnboardingException(Exception e) {
    super(e);
  }

  public OnboardingException(String message) {
    super(message);
  }

  public OnboardingException(String message, Exception e) {
    super(message, e);
  }
}
