package io.harness.exception.runtime;

public class HashiCorpVaultRuntimeException extends RuntimeException {
  private String message;
  public HashiCorpVaultRuntimeException(String message) {
    super(message);
    this.message = message;
  }
}