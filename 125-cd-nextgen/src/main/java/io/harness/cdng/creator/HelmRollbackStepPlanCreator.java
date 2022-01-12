/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator;

import static io.harness.cdng.visitor.YamlTypes.HELM_DEPLOY;

import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.cdng.helm.rollback.HelmRollbackStepParams;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.WithStepElementParameters;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.utils.TimeoutUtils;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;

public class HelmRollbackStepPlanCreator extends GenericStepPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.HELM_ROLLBACK);
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
    String nativeHelmFqn = getExecutionStepFqn(ctx.getCurrentField(), HELM_DEPLOY);
    ((HelmRollbackStepParams) ((StepElementParameters) stepParameters).getSpec()).setHelmRollbackFqn(nativeHelmFqn);
    return stepParameters;
  }
}
