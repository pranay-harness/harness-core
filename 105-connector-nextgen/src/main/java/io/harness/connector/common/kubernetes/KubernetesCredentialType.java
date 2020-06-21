package io.harness.connector.common.kubernetes;

public enum KubernetesCredentialType {
  INHERIT_FROM_DELEGATE("InheritFromDelegate"),
  MANUAL_CREDENTIALS("ManualConfig");

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
}
