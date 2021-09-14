/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config.k8s.recommendation.estimators;

import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.cpu;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.makeResourceMap;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.memory;

import static java.lang.Math.max;

import io.harness.batch.processing.config.k8s.recommendation.ContainerState;

import java.util.Map;
import lombok.Value;

/**
 * Makes sure the estimation from underlying estimator is at least minResources.
 */
@Value(staticConstructor = "of")
class MinResourceEstimator implements ResourceEstimator {
  Map<String, Long> minResources;
  ResourceEstimator baseEstimator;

  @Override
  public Map<String, Long> getResourceEstimation(ContainerState containerState) {
    Map<String, Long> original = baseEstimator.getResourceEstimation(containerState);
    return makeResourceMap(max(cpu(original), cpu(minResources)), max(memory(original), memory(minResources)));
  }
}
