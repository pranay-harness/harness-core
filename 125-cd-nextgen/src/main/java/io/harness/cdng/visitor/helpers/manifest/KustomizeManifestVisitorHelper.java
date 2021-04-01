package io.harness.cdng.visitor.helpers.manifest;

import io.harness.cdng.manifest.yaml.kinds.KustomizeManifest;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class KustomizeManifestVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {}

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    KustomizeManifest kustomizeManifest = (KustomizeManifest) originalElement;
    return KustomizeManifest.builder().identifier(kustomizeManifest.getIdentifier()).build();
  }
}
