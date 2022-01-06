/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps;

import static io.harness.cdng.visitor.YamlTypes.K8S_BG_SWAP_SERVICES;
import static io.harness.cdng.visitor.YamlTypes.K8S_BLUE_GREEN_DEPLOY;

import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.k8s.K8sBGSwapServicesStepParameters;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.WithStepElementParameters;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.utils.TimeoutUtils;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class K8sBGSwapServicesPMSStepPlanCreator extends K8sRetryAdviserObtainment {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("K8sBGSwapServices");
  }

  @Override
  protected StepParameters getStepParameters(PlanCreationContext ctx, StepElementConfig stepElement) {
    StepParameters stepParameters = stepElement.getStepSpecType().getStepParameters();
    if (stepElement.getStepSpecType() instanceof WithStepElementParameters) {
      stepElement.setTimeout(TimeoutUtils.getTimeout(stepElement.getTimeout()));
      stepParameters =
          ((WithStepElementParameters) stepElement.getStepSpecType())
              .getStepParametersInfo(stepElement,
                  getRollbackParameters(ctx.getCurrentField(), Collections.emptySet(), RollbackStrategy.UNKNOWN));
    }

    String bgStepFqn = getExecutionStepFqn(ctx.getCurrentField(), K8S_BLUE_GREEN_DEPLOY);
    String bgSwapServicesStepFqn = getExecutionStepFqn(ctx.getCurrentField(), K8S_BG_SWAP_SERVICES);
    K8sBGSwapServicesStepParameters k8sBGSwapServicesStepParameters =
        (K8sBGSwapServicesStepParameters) ((StepElementParameters) stepParameters).getSpec();
    k8sBGSwapServicesStepParameters.setBlueGreenStepFqn(bgStepFqn);
    k8sBGSwapServicesStepParameters.setBlueGreenSwapServicesStepFqn(bgSwapServicesStepFqn);

    return stepParameters;
  }
}
