/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config.k8s.recommendation.estimators;

import io.harness.batch.processing.config.k8s.recommendation.ContainerState;

import java.util.Map;
import java.util.Set;

interface ResourceEstimator {
  // cpu in millicores, memory in bytes
  Map<String, Long> getResourceEstimation(ContainerState containerState);

  default ResourceEstimator withMargin(double marginFraction) {
    return MarginEstimator.of(marginFraction, this);
  }

  default ResourceEstimator withMinResources(Map<String, Long> minResources) {
    return MinResourceEstimator.of(minResources, this);
  }

  default ResourceEstimator withConfidenceMultiplier(double multiplier, double exponent) {
    return ConfidenceMultiplierEstimator.of(multiplier, exponent, this);
  }

  default ResourceEstimator omitResources(String... omittedResources) {
    return new OmitResourceEstimator(omittedResources, this);
  }

  default ResourceEstimator omitResources(Set<String> noLimitResources) {
    return new OmitResourceEstimator(noLimitResources, this);
  }
}
