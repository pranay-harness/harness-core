/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.service.steps;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.VariablesSweepingOutput;
import io.harness.cdng.manifest.steps.ManifestsOutcome;
import io.harness.ngpipeline.artifact.bean.ArtifactsOutcome;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("serviceConfigOutcome")
@JsonTypeName("serviceConfigOutcome")
@RecasterAlias("io.harness.cdng.service.steps.ServiceConfigStepOutcome")
public class ServiceConfigStepOutcome implements Outcome {
  ServiceStepOutcome serviceResult;
  VariablesSweepingOutput variablesResult;
  ArtifactsOutcome artifactResults;
  ManifestsOutcome manifestResults;
}
