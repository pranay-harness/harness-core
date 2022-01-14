package io.harness.cvng.exception;

public class CVWebhookException extends RuntimeException {
  public CVWebhookException(Exception e) {
    super(e);
  }

  public CVWebhookException(String message) {
    super(message);
  }

  public CVWebhookException(String message, Exception e) {
    super(message, e);
  }
}
