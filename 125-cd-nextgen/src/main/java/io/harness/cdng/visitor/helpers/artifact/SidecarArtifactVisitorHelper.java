package io.harness.cdng.visitor.helpers.artifact;

import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class SidecarArtifactVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement() {
    return SidecarArtifact.builder().build();
  }
}
