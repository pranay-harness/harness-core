package io.harness.cdng.visitor.helpers.pipelineinfrastructure;

import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class EnvironmentYamlVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return EnvironmentYaml.builder().build();
  }
}
