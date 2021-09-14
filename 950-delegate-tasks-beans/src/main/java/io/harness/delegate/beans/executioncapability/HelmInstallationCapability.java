/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.executioncapability;

import io.harness.k8s.model.HelmVersion;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HelmInstallationCapability implements ExecutionCapability {
  HelmVersion version;
  String criteria;
  CapabilityType capabilityType = CapabilityType.HELM_INSTALL;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return criteria;
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
