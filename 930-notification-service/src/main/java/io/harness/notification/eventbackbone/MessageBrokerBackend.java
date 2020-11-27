package io.harness.notification.eventbackbone;

public enum MessageBrokerBackend {
  KAFKA("KAFKA"),
  MONGO("MONGO");

  private String backend;

  MessageBrokerBackend(String backend) {
    this.backend = backend;
  }

  public String getBackend() {
    return backend;
  }
}
