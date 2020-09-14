package io.harness.cdng.visitor.helpers.pipelineinfrastructure;

import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class K8SDirectInfrastructureVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return K8SDirectInfrastructure.builder().build();
  }
}
