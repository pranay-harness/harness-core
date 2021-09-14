/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.cdOverview.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@Builder
@OwnedBy(DX)
public class ServiceDeploymentListInfo {
  Long startTime;
  Long endTime;
  Long totalDeployments;
  double failureRate;
  double frequency;
  double failureRateChangeRate;
  double totalDeploymentsChangeRate;
  double frequencyChangeRate;
  List<ServiceDeployment> serviceDeploymentList;
}
