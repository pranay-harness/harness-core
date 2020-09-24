package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GcpCredentialType {
  @JsonProperty(GcpConstants.inheritFromDelegate) INHERIT_FROM_DELEGATE(GcpConstants.inheritFromDelegate),
  @JsonProperty(GcpConstants.manualConfig) MANUAL_CREDENTIALS(GcpConstants.manualConfig);

  private final String displayName;

  GcpCredentialType(String displayName) {
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
