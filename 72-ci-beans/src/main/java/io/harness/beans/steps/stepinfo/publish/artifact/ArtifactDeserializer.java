package io.harness.beans.steps.stepinfo.publish.artifact;

import io.harness.yaml.core.deserializer.PropertyBindingPolymorphicDeserializer;

public class ArtifactDeserializer extends PropertyBindingPolymorphicDeserializer<Artifact> {
  public ArtifactDeserializer() {
    super(Artifact.class);
    init();
  }
  public ArtifactDeserializer(Class<Artifact> clazz) {
    super(clazz);
    init();
  }

  private void init() {
    registerBinding(Artifact.FILE_PATTERN_PROPERTY, FilePatternArtifact.class);
    registerBinding(Artifact.DOCKER_FILE_PROPERTY, DockerFileArtifact.class);
    registerBinding(Artifact.DOCKER_IMAGE_PROPERTY, DockerImageArtifact.class);
  }
}
