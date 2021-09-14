/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
