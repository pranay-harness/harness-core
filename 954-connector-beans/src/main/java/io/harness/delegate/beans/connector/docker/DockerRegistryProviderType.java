package io.harness.delegate.beans.connector.docker;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(CDC)
public enum DockerRegistryProviderType {
  @JsonProperty(DockerConstants.DOCKER_HUB) DOCKER_HUB(DockerConstants.DOCKER_HUB),
  @JsonProperty(DockerConstants.HARBOR) HARBOR(DockerConstants.HARBOR),
  @JsonProperty(DockerConstants.QUAY) QUAY(DockerConstants.QUAY),
  @JsonProperty(DockerConstants.OTHER) OTHER(DockerConstants.OTHER);

  private final String displayName;

  DockerRegistryProviderType(String displayName) {
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
