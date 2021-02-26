package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum KubernetesCredentialType {
  @JsonProperty("InheritFromDelegate") INHERIT_FROM_DELEGATE("InheritFromDelegate"),
  @JsonProperty("ManualConfig") MANUAL_CREDENTIALS("ManualConfig");

  private final String displayName;

  KubernetesCredentialType(String displayName) {
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
