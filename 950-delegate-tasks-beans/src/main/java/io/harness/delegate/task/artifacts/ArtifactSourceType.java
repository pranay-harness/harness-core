package io.harness.delegate.task.artifacts;

import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.DOCKER_REGISTRY_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.ECR_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.GCR_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

@OwnedBy(HarnessTeam.CDC)
public enum ArtifactSourceType {
  @JsonProperty(DOCKER_REGISTRY_NAME) DOCKER_REGISTRY(DOCKER_REGISTRY_NAME),
  @JsonProperty(GCR_NAME) GCR(GCR_NAME),
  @JsonProperty(ECR_NAME) ECR(ECR_NAME);
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

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static ArtifactSourceType getArtifactSourceType(@JsonProperty("type") String displayName) {
    for (ArtifactSourceType sourceType : ArtifactSourceType.values()) {
      if (sourceType.displayName.equalsIgnoreCase(displayName)) {
        return sourceType;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Invalid value:%s, the expected values are: %s", displayName, Arrays.toString(ArtifactSourceType.values())));
  }
}
