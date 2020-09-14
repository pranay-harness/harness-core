package io.harness.cdng.visitor.helpers.artifact;

import io.harness.cdng.artifact.bean.ArtifactSpecWrapper;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class ArtifactSpecWrapperVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    ArtifactSpecWrapper original = (ArtifactSpecWrapper) originalElement;
    return ArtifactSpecWrapper.builder().sourceType(original.getSourceType()).build();
  }
}
