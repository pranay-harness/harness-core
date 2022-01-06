/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.executions.steps.StepSpecTypeConstants;

import com.google.common.collect.Sets;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class CDCreatorUtils {
  public Set<String> getSupportedSteps() {
    return Sets.newHashSet("K8sRollingDeploy", "K8sRollingRollback", "K8sScale", "K8sBlueGreenDeploy",
        "K8sBGSwapServices", "K8sDelete", "K8sCanaryDelete", "K8sApply", "TerraformApply", "TerraformPlan",
        "TerraformDestroy", StepSpecTypeConstants.TERRAFORM_ROLLBACK, StepSpecTypeConstants.HELM_DEPLOY,
        StepSpecTypeConstants.HELM_ROLLBACK);
  }
  public Set<String> getSupportedStepsV2() {
    return Sets.newHashSet("K8sCanaryDeploy");
  }
}
