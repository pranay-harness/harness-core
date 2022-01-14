package io.harness.ngtriggers.beans.source.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum ArtifactType {
  @JsonProperty("Gcr") GCR("Gcr"),
  @JsonProperty("Ecr") ECR("Ecr"),
  @JsonProperty("DockerRegistry") DOCKER_REGISTRY("DockerRegistry");

  private String value;

  ArtifactType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
