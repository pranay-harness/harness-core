package io.harness.cdng.visitor.helpers.artifact;

import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class DockerHubArtifactConfigVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    DockerHubArtifactConfig dockerHubArtifactConfig = (DockerHubArtifactConfig) originalElement;
    return DockerHubArtifactConfig.builder().identifier(dockerHubArtifactConfig.getIdentifier()).build();
  }
}
