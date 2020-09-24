package io.harness.cdng.visitor.helpers.serviceconfig;

import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class ServiceDefinitionVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    ServiceDefinition serviceDefinition = (ServiceDefinition) originalElement;
    return ServiceDefinition.builder().type(serviceDefinition.getType()).build();
  }
}
