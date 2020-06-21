package io.harness.connector.common.kubernetes;

public enum KubernetesAuthType {
  USER_PASSWORD("UsernamePassword"),
  CLIENT_KEY_CERT("ClientKeyCert"),
  SERVICE_ACCOUNT("ServiceAccount"),
  OPEN_ID_CONNECT("OpenIdConnect");

  private final String displayName;

  KubernetesAuthType(String displayName) {
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
