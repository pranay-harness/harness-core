/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config.k8s.recommendation.estimators;

import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.cpuAmountFromCores;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.makeResourceMap;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.memoryAmountFromBytes;

import io.harness.batch.processing.config.k8s.recommendation.ContainerState;

import java.util.Map;
import lombok.Value;

/**
 * Returns an estimation based on specific percentiles of cpu usage & memory peak distribution.
 */
@Value(staticConstructor = "of")
class PercentileEstimator implements ResourceEstimator {
  double cpuPercentile;
  double memoryPercentile;

  @Override
  public Map<String, Long> getResourceEstimation(ContainerState containerState) {
    return makeResourceMap(cpuAmountFromCores(containerState.getCpuHistogram().getPercentile(cpuPercentile)),
        memoryAmountFromBytes(containerState.getMemoryHistogram().getPercentile(memoryPercentile)));
  }
}
