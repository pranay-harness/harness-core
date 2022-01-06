/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
