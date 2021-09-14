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

@Value
@Builder
@OwnedBy(CDP)
@TypeAlias("k8sCanaryOutcome")
@JsonTypeName("k8sCanaryOutcome")
@RecasterAlias("io.harness.cdng.k8s.K8sCanaryOutcome")
public class K8sCanaryOutcome implements Outcome, ExecutionSweepingOutput {
  String releaseName;
  Integer targetInstances;
  Integer releaseNumber;
  String canaryWorkload;
  boolean canaryWorkloadDeployed;
}
