package io.harness.connector.common;

import com.fasterxml.jackson.annotation.JsonValue;

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

  @JsonValue
  final String displayName() {
    return this.displayName;
  }
}