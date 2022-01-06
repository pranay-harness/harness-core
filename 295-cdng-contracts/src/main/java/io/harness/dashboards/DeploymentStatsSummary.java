/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
public class DeploymentStatsSummary {
  long totalCount;
  @Builder.Default double totalCountChangeRate = DashboardHelper.MAX_VALUE;
  @Builder.Default double failureRate = DashboardHelper.MAX_VALUE;
  @Builder.Default double failureRateChangeRate = DashboardHelper.MAX_VALUE;
  @Builder.Default double deploymentRate = DashboardHelper.MAX_VALUE;
  @Builder.Default double deploymentRateChangeRate = DashboardHelper.MAX_VALUE;

  @Builder.Default List<TimeBasedDeploymentInfo> timeBasedDeploymentInfoList = new ArrayList<>();
}
