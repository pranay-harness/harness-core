package io.harness.visitor.helpers.stage;

import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;
import io.harness.yaml.core.StageElement;

public class StageElementHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Will be taken by Validation framework impl.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    StageElement stageElement = (StageElement) originalElement;
    return StageElement.builder().identifier(stageElement.getIdentifier()).type(stageElement.getType()).build();
  }
}
