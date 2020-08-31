package io.harness.delegate.task.artifacts;

import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.DOCKER_HUB_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.GCR_NAME;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ArtifactSourceType {
  @JsonProperty(DOCKER_HUB_NAME) DOCKER_HUB(DOCKER_HUB_NAME),
  @JsonProperty(GCR_NAME) GCR(GCR_NAME);
  private final String displayName;

  ArtifactSourceType(String displayName) {
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

  @JsonCreator
  public static ArtifactSourceType getArtifactSourceType(@JsonProperty("type") String displayName) {
    for (ArtifactSourceType sourceType : ArtifactSourceType.values()) {
      if (sourceType.displayName.equalsIgnoreCase(displayName)) {
        return sourceType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }
}
