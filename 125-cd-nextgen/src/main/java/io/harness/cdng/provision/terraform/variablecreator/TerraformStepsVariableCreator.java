/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.provision.terraform.variablecreator;

import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TerraformStepsVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return new HashSet<>(Arrays.asList(StepSpecTypeConstants.TERRAFORM_APPLY, StepSpecTypeConstants.TERRAFORM_PLAN,
        StepSpecTypeConstants.TERRAFORM_DESTROY, StepSpecTypeConstants.TERRAFORM_ROLLBACK));
  }
}
