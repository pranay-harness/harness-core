package io.harness.delegate.beans.connector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(HarnessTeam.GITOPS)
public enum GitOpsProviderType {
  @JsonProperty("ConnectedArgoProvider") CONNECTED_ARGO_PROVIDER("ConnectedArgoProvider"),
  @JsonProperty("ManagedArgoProvider") MANAGED_ARGO_PROVIDER("ManagedArgoProvider");
  private final String displayName;

  GitOpsProviderType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }
}
