package io.harness.cdng.visitor.helpers.manifest;

import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class ManifestConfigVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    ManifestConfig manifestConfig = (ManifestConfig) originalElement;
    return ManifestConfig.builder().identifier(manifestConfig.getIdentifier()).type(manifestConfig.getType()).build();
  }
}
