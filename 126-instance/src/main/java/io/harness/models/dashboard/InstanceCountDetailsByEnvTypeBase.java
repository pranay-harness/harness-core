/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.models.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.EnvironmentType;

import java.util.Map;
import lombok.Builder;

@Builder
@OwnedBy(HarnessTeam.DX)
public class InstanceCountDetailsByEnvTypeBase {
  private Map<EnvironmentType, Integer> envTypeVsInstanceCountMap;

  public Integer getNonProdInstances() {
    return envTypeVsInstanceCountMap.getOrDefault(EnvironmentType.PreProduction, 0);
  }

  public Integer getProdInstances() {
    return envTypeVsInstanceCountMap.getOrDefault(EnvironmentType.Production, 0);
  }

  public Integer getTotalInstances() {
    return getNonProdInstances() + getProdInstances();
  }
}
