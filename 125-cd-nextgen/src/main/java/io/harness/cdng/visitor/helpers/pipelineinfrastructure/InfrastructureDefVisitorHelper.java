package io.harness.cdng.visitor.helpers.pipelineinfrastructure;

import io.harness.cdng.infra.InfrastructureDef;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class InfrastructureDefVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return InfrastructureDef.builder().build();
  }
}
