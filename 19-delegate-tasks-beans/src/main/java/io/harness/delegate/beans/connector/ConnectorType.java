package io.harness.delegate.beans.connector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConnectorType {
  KUBERNETES_CLUSTER("K8sCluster"),
  GIT("Git"),
  SPLUNK("Splunk"),
  APP_DYNAMICS("AppDynamics");

  private final String displayName;

  @JsonCreator
  public static ConnectorType fromDisplayName(@JsonProperty("type") String displayName) {
    for (ConnectorType connectorType : ConnectorType.values()) {
      if (connectorType.displayName.equalsIgnoreCase(displayName)) {
        return connectorType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }

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