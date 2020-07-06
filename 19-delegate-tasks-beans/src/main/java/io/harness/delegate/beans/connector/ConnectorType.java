package io.harness.delegate.beans.connector;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConnectorType {
  KUBERNETES_CLUSTER("K8sCluster");

  private final String displayName;

  ConnectorType(String displayName) {
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