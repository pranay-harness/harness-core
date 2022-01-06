/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.helper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.DeploymentSummaryDTO;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DX)
public class InstanceSyncLocalCacheManager {
  private final Cache<String, DeploymentSummaryDTO> deploymentSummaryCache =
      Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

  public void setDeploymentSummary(String key, DeploymentSummaryDTO deploymentSummaryDTO) {
    deploymentSummaryCache.put(key, deploymentSummaryDTO);
  }

  public DeploymentSummaryDTO getDeploymentSummary(String key) {
    return deploymentSummaryCache.getIfPresent(key);
  }

  public void removeDeploymentSummary(String key) {
    if (key != null) {
      deploymentSummaryCache.invalidate(key);
    }
  }
}
