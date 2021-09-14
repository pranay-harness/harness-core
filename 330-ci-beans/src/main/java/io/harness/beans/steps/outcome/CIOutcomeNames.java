/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.steps.outcome;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CI)
public class CIOutcomeNames {
  public static final String CI_STEP_OUTCOME = "ciStepOutcome";
  public static final String CI_STEP_ARTIFACT_OUTCOME = "ciStepArtifact";
  public static final String INTEGRATION_STAGE_OUTCOME = "integrationStageOutcome";
}
