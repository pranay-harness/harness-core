package io.harness.cdng.visitor.helpers.artifact;

import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class GcrArtifactConfigVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    GcrArtifactConfig gcrArtifactConfig = (GcrArtifactConfig) originalElement;
    return GcrArtifactConfig.builder().identifier(gcrArtifactConfig.getIdentifier()).build();
  }
}
