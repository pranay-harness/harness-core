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
  public Object createDummyVisitableElement() {
    return DockerHubArtifactConfig.builder().build();
  }
}
