package io.harness.beans.steps.stepinfo.publish.artifact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ArtifactDeserializer.class)
public interface Artifact {
  ArtifactType getType();
}
