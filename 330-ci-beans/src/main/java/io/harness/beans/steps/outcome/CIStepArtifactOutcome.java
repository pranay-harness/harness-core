/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.steps.outcome;

import static io.harness.beans.steps.outcome.CIOutcomeNames.CI_STEP_ARTIFACT_OUTCOME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias(CI_STEP_ARTIFACT_OUTCOME)
@JsonTypeName(CI_STEP_ARTIFACT_OUTCOME)
@OwnedBy(HarnessTeam.CI)
@RecasterAlias("io.harness.beans.steps.outcome.CIStepArtifactOutcome")
public class CIStepArtifactOutcome implements Outcome {
  StepArtifacts stepArtifacts;
}
