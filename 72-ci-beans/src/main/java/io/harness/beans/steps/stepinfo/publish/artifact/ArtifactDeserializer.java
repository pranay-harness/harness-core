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
    registerBinding(ArtifactType.FILE_PATTERN.getPropertyName(), FilePatternArtifact.class);
    registerBinding(ArtifactType.DOCKER_FILE.getPropertyName(), DockerFileArtifact.class);
    registerBinding(ArtifactType.DOCKER_IMAGE.getPropertyName(), DockerImageArtifact.class);
  }
}
