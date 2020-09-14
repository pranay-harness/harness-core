package io.harness.cdng.visitor.helper;

import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class GitStoreVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement() {
    return GitStore.builder().build();
  }
}
