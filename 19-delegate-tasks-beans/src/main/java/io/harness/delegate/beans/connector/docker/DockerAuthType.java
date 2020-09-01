package io.harness.delegate.beans.connector.docker;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DockerAuthType {
  @JsonProperty("UsernamePassword") USER_PASSWORD("UsernamePassword"),
  NO_AUTH("NoAuth");

  private final String displayName;

  DockerAuthType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  @JsonValue
  final String displayName() {
    return this.displayName;
  }
}
