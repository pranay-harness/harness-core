/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config.k8s.recommendation.estimators;

import io.harness.batch.processing.config.k8s.recommendation.ContainerState;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OmitResourceEstimator implements ResourceEstimator {
  private final Set<String> omittedResources;
  private final ResourceEstimator baseEstimator;

  public OmitResourceEstimator(String[] omittedResources, ResourceEstimator baseEstimator) {
    this(new HashSet<>(Arrays.asList(omittedResources)), baseEstimator);
  }

  public OmitResourceEstimator(Set<String> omittedResources, ResourceEstimator baseEstimator) {
    this.omittedResources = omittedResources;
    this.baseEstimator = baseEstimator;
  }

  @Override
  public Map<String, Long> getResourceEstimation(ContainerState containerState) {
    Map<String, Long> baseEstimation = baseEstimator.getResourceEstimation(containerState);
    return baseEstimation.entrySet()
        .stream()
        .filter(resourceEntry -> !omittedResources.contains(resourceEntry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
