/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.creator.variables;

import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class K8sStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return new HashSet<>(Arrays.asList(StepSpecTypeConstants.K8S_ROLLING_DEPLOY,
        StepSpecTypeConstants.K8S_ROLLING_ROLLBACK, StepSpecTypeConstants.K8S_BLUE_GREEN_DEPLOY,
        StepSpecTypeConstants.K8S_APPLY, StepSpecTypeConstants.K8S_SCALE, StepSpecTypeConstants.K8S_BG_SWAP_SERVICES,
        StepSpecTypeConstants.K8S_CANARY_DELETE, StepSpecTypeConstants.K8S_CANARY_DEPLOY,
        StepSpecTypeConstants.K8S_DELETE));
  }
}
