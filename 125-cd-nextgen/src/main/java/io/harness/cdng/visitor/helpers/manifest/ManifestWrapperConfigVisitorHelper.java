package io.harness.cdng.visitor.helpers.manifest;

import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class ManifestWrapperConfigVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return ManifestConfigWrapper.builder().build();
  }
}
