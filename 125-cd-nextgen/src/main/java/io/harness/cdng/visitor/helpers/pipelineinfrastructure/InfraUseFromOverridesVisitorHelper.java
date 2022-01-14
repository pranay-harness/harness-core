package io.harness.cdng.visitor.helpers.pipelineinfrastructure;

import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class InfraUseFromOverridesVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return InfraUseFromStage.Overrides.builder().build();
  }
}
