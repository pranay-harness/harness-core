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
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.scaleResourceAmount;

import io.harness.batch.processing.config.k8s.recommendation.ContainerState;

import java.util.Map;
import lombok.Value;

/**
 * Adds a safety margin to the estimation from underlying estimator.
 */
@Value(staticConstructor = "of")
class MarginEstimator implements ResourceEstimator {
  double marginFraction;
  ResourceEstimator baseEstimator;

  @Override
  public Map<String, Long> getResourceEstimation(ContainerState containerState) {
    Map<String, Long> original = baseEstimator.getResourceEstimation(containerState);
    return makeResourceMap(scaleResourceAmount(cpu(original), 1 + marginFraction),
        scaleResourceAmount(memory(original), 1 + marginFraction));
  }
}
