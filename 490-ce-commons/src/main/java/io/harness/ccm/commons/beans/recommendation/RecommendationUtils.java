/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.beans.recommendation;

import io.harness.ccm.commons.beans.recommendation.models.RecommendClusterRequest;
import io.harness.exception.InvalidRequestException;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RecommendationUtils {
  public RecommendClusterRequest constructNodeRecommendationRequest(TotalResourceUsage totalResourceUsage) {
    if (!isResourceConsistent(totalResourceUsage)) {
      throw new InvalidRequestException(String.format("Inconsistent TotalResourceUsage: %s", totalResourceUsage));
    }

    long minNodes = 3L;
    long maxNodesPossible = (long) Math.min(Math.floor(totalResourceUsage.getSumcpu() / totalResourceUsage.getMaxcpu()),
        Math.floor(totalResourceUsage.getSummemory() / totalResourceUsage.getMaxmemory()));

    maxNodesPossible = Math.max(maxNodesPossible, 1L);
    if (maxNodesPossible < 3L) {
      minNodes = maxNodesPossible;
    }

    return RecommendClusterRequest.builder()
        .maxNodes(maxNodesPossible)
        .minNodes(minNodes)
        .sumCpu(totalResourceUsage.getSumcpu() / 1024.0D)
        .sumMem(totalResourceUsage.getSummemory() / 1024.0D)
        .allowBurst(true)
        .sameSize(true)
        .build();
  }

  private boolean isResourceConsistent(@NonNull TotalResourceUsage resource) {
    boolean inconsistent = Math.round(resource.getSumcpu()) < Math.round(resource.getMaxcpu())
        || Math.round(resource.getSummemory()) < Math.round(resource.getMaxmemory());
    boolean anyZero = Math.round(resource.getSumcpu()) == 0L || Math.round(resource.getSummemory()) == 0L
        || Math.round(resource.getMaxcpu()) == 0L || Math.round(resource.getMaxmemory()) == 0L;

    return !inconsistent && !anyZero;
  }
}
