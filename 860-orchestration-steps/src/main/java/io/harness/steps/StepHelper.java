/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.ng.core.environment.beans.EnvironmentType.Production;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.steps.environment.EnvironmentOutcome;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDP)
@Singleton
public class StepHelper {
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  public EnvironmentType getEnvironmentType(Ambiance ambiance) {
    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputResolver.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(OutputExpressionConstants.ENVIRONMENT));
    if (!optionalSweepingOutput.isFound()) {
      return EnvironmentType.ALL;
    }

    EnvironmentOutcome envOutcome = (EnvironmentOutcome) optionalSweepingOutput.getOutput();

    if (envOutcome == null || envOutcome.getType() == null) {
      return EnvironmentType.ALL;
    }

    return Production == envOutcome.getType() ? EnvironmentType.PROD : EnvironmentType.NON_PROD;
  }
}
