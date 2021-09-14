/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngpipeline.artifact.bean;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@JsonTypeName("ArtifactsOutcome")
@TypeAlias("artifactsOutcome")
@RecasterAlias("io.harness.ngpipeline.artifact.bean.ArtifactsOutcome")
public class ArtifactsOutcome implements Outcome {
  ArtifactOutcome primary;
  SidecarsOutcome sidecars;
}
