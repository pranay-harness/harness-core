package io.harness.beans.steps.stepinfo.publish.artifact;

import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

@JsonDeserialize(using = ArtifactDeserializer.class)
public interface Artifact {
  String FILE_PATTERN_PROPERTY = "filePattern";
  String DOCKER_FILE_PROPERTY = "dockerFile";
  String DOCKER_IMAGE_PROPERTY = "dockerImage";

  enum Type {
    FILE_PATTERN(FILE_PATTERN_PROPERTY),
    DOCKER_FILE(DOCKER_FILE_PROPERTY),
    DOCKER_IMAGE(DOCKER_IMAGE_PROPERTY);

    @Getter private final String propertyName;
    Type(String propertyName) {
      this.propertyName = propertyName;
    }
  }

  Type getType();
  ArtifactConnector getConnector();
}
