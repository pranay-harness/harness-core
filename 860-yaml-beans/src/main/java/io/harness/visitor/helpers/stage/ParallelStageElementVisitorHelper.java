package io.harness.visitor.helpers.stage;

import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;
import io.harness.yaml.core.ParallelStageElement;

public class ParallelStageElementVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return ParallelStageElement.builder().build();
  }
}
