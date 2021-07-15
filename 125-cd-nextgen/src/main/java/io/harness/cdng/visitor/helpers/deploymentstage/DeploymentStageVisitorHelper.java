package io.harness.cdng.visitor.helpers.deploymentstage;

import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class DeploymentStageVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return DeploymentStageConfig.builder().build();
  }
}
