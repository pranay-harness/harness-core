package io.harness.delegate.beans.connector.nexusconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NexusAuthType {
  @JsonProperty(NexusConstants.USERNAME_PASSWORD) USER_PASSWORD(NexusConstants.USERNAME_PASSWORD),
  @JsonProperty(NexusConstants.ANONYMOUS) ANONYMOUS(NexusConstants.ANONYMOUS);

  private final String displayName;

  NexusAuthType(String displayName) {
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

  public static NexusAuthType fromString(String typeEnum) {
    for (NexusAuthType enumValue : NexusAuthType.values()) {
      if (enumValue.getDisplayName().equals(typeEnum)) {
        return enumValue;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + typeEnum);
  }
}
