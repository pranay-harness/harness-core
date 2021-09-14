/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.visitor.helpers.deploymentstage;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

@OwnedBy(CDC)
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
