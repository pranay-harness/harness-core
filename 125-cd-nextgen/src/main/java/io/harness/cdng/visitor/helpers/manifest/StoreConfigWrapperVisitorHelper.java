package io.harness.cdng.visitor.helpers.manifest;

import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class StoreConfigWrapperVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    StoreConfigWrapper storeConfigWrapper = (StoreConfigWrapper) originalElement;
    return StoreConfigWrapper.builder().type(storeConfigWrapper.getType()).build();
  }
}
