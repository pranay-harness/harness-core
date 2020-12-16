package io.harness.delegate.beans.connector.docker;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DockerAuthType {
  @JsonProperty(DockerConstants.USERNAME_PASSWORD) USER_PASSWORD(DockerConstants.USERNAME_PASSWORD),
  @JsonProperty(DockerConstants.ANONYMOUS) ANONYMOUS(DockerConstants.ANONYMOUS);

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

  public static DockerAuthType fromString(String typeEnum) {
    for (DockerAuthType enumValue : DockerAuthType.values()) {
      if (enumValue.getDisplayName().equals(typeEnum)) {
        return enumValue;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + typeEnum);
  }
}
