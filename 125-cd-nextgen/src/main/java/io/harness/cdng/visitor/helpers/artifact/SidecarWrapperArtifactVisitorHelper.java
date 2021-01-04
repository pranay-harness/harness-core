package io.harness.cdng.visitor.helpers.artifact;

import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class SidecarWrapperArtifactVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return SidecarArtifactWrapper.builder().build();
  }
}
