package io.harness.cdng.visitor.helpers.manifest;

import io.harness.cdng.manifest.yaml.kinds.OpenshiftManifest;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class OpenshiftManifestVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {}

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    OpenshiftManifest openshiftManifest = (OpenshiftManifest) originalElement;
    return OpenshiftManifest.builder().identifier(openshiftManifest.getIdentifier()).build();
  }
}
