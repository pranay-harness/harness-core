/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("k8sBlueGreenOutcome")
@JsonTypeName("k8sBlueGreenOutcome")
@RecasterAlias("io.harness.cdng.k8s.K8sBlueGreenOutcome")
public class K8sBlueGreenOutcome implements Outcome, ExecutionSweepingOutput {
  int releaseNumber;
  String releaseName;
  String primaryServiceName;
  String stageServiceName;
  String stageColor;
  String primaryColor;
}
