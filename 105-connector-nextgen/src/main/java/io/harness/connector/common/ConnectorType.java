package io.harness.connector.common;

public enum ConnectorType {
  KUBERNETES_CLUSTER("K8sCluster");

  private final String displayName;

  ConnectorType(String displayName) {
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