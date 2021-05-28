package io.harness.delegate.task.artifacts;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(HarnessTeam.PIPELINE)
public enum ArtifactTaskType {
  GET_BUILDS("Get Builds"),
  GET_BUILD_NUMBER("Get Build Number"),
  GET_LAST_SUCCESSFUL_BUILD("Get last successful Build"),
  VALIDATE_ARTIFACT_SERVER("Validate Artifact Server"),
  VALIDATE_ARTIFACT_SOURCE("Validate Artifact Source"),
  GET_LABELS("Get Labels"),
  GET_PLANS("Get Plans"),
  GET_FEEDS("Get Feeds"),
  GET_IMAGE_URL("Get Image URL"),
  GET_AUTH_TOKEN("Get Auth Token"),
  GET_IMAGES("Get Images");

  @Getter private final String displayName;

  ArtifactTaskType(String displayName) {
    this.displayName = displayName;
  }
}
