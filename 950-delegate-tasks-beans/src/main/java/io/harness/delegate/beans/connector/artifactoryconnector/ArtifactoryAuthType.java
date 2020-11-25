package io.harness.delegate.beans.connector.artifactoryconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ArtifactoryAuthType {
  @JsonProperty(ArtifactoryConstants.USERNAME_PASSWORD) USER_PASSWORD(ArtifactoryConstants.USERNAME_PASSWORD),
  NO_AUTH(ArtifactoryConstants.NO_AUTH);

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
