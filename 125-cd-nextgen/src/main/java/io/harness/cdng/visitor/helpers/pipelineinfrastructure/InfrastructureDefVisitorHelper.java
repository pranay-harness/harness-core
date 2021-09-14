/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
    InfrastructureDef infrastructureDef = (InfrastructureDef) originalElement;
    return InfrastructureDef.builder().type(infrastructureDef.getType()).build();
  }
}
