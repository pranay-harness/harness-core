/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps;

import static io.harness.cdng.visitor.YamlTypes.K8S_CANARY_DEPLOY;

import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.k8s.K8sRollingStepParameters;
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
public class K8sRollingDeployPMSStepPlanCreator extends K8sRetryAdviserObtainment {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("K8sRollingDeploy");
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

    String canaryStepFqn = getExecutionStepFqn(ctx.getCurrentField(), K8S_CANARY_DEPLOY);
    ((K8sRollingStepParameters) ((StepElementParameters) stepParameters).getSpec()).setCanaryStepFqn(canaryStepFqn);

    return stepParameters;
  }
}
