/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.overview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkloadDeploymentInfo {
  String serviceName;
  String serviceId;
  LastWorkloadInfo lastExecuted;
  Set<String> deploymentTypeList;
  long totalDeployments;
  double totalDeploymentChangeRate;
  double percentSuccess;
  double rateSuccess;
  double failureRate;
  double failureRateChangeRate;
  double frequency;
  double frequencyChangeRate;
  String lastPipelineExecutionId;
  List<WorkloadDateCountInfo> workload;
}
