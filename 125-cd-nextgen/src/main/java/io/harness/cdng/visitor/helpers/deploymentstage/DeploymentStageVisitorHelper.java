package io.harness.cdng.visitor.helpers.deploymentstage;

import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class DeploymentStageVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement() {
    return DeploymentStage.builder().build();
  }
}
