/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.overview.dto;

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
