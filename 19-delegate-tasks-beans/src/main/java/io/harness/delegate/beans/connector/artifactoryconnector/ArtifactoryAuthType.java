package io.harness.delegate.beans.connector.artifactoryconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ArtifactoryAuthType {
  @JsonProperty(ArtifactoryConstants.usernamePassword) USER_PASSWORD(ArtifactoryConstants.usernamePassword),
  NO_AUTH(ArtifactoryConstants.noAuth);

  private final String displayName;

  ArtifactoryAuthType(String displayName) {
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
