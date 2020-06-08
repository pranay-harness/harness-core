package io.harness.beans.steps.stepinfo.publish.artifact;

import lombok.Getter;

public enum ArtifactType {
  FILE_PATTERN("filePattern"),
  DOCKER_FILE("dockerFile"),
  DOCKER_IMAGE("dockerImage");

  @Getter private final String propertyName;

  ArtifactType(String propertyName) {
    this.propertyName = propertyName;
  }
}
