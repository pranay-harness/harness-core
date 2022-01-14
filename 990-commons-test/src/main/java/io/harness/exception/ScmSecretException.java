package io.harness.exception;

public class ScmSecretException extends RuntimeException {
  public ScmSecretException(Exception cause) {
    super(cause);
  }
}
