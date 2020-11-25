package io.harness.delegate.beans.azure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AzureVMAuthType {
  @JsonProperty("SshPublicKey") SSH_PUBLIC_KEY("SshPublicKey"),
  @JsonProperty("Credentials") PASSWORD("Credentials");

  private final String displayName;

  AzureVMAuthType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
