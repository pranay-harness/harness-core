package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum KubernetesCredentialType {
  @JsonProperty("InheritFromDelegate") INHERIT_FROM_DELEGATE("InheritFromDelegate", false),
  @JsonProperty("ManualConfig") MANUAL_CREDENTIALS("ManualConfig", true);

  private final String displayName;
  private final boolean decryptable;

  KubernetesCredentialType(String displayName, boolean decryptable) {
    this.displayName = displayName;
    this.decryptable = decryptable;
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

  @JsonIgnore
  public boolean isDecryptable() {
    return decryptable;
  }
}
