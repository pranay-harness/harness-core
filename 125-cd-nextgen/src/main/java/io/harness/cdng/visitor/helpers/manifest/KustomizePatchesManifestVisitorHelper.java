package io.harness.cdng.visitor.helpers.manifest;

import io.harness.cdng.manifest.yaml.kinds.KustomizePatchesManifest;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class KustomizePatchesManifestVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    KustomizePatchesManifest kustomizePatchesManifest = (KustomizePatchesManifest) originalElement;
    return KustomizePatchesManifest.builder().identifier(kustomizePatchesManifest.getIdentifier()).build();
  }
}