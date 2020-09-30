package io.harness.delegate.beans.connector.docker;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DockerAuthType {
  @JsonProperty(DockerConstants.USERNAME_PASSWORD) USER_PASSWORD(DockerConstants.USERNAME_PASSWORD),
  NO_AUTH(DockerConstants.NO_AUTH);

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
