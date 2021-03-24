package io.harness.cdng.visitor.helpers.manifest;

import io.harness.cdng.manifest.yaml.kinds.OpenshiftParamManifest;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class OpenshiftParamManifestVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {}

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    OpenshiftParamManifest openshiftParamManifest = (OpenshiftParamManifest) originalElement;
    return OpenshiftParamManifest.builder().identifier(openshiftParamManifest.getIdentifier()).build();
  }
}
