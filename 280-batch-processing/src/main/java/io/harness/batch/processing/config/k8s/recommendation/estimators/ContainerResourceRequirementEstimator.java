/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config.k8s.recommendation.estimators;

import io.harness.batch.processing.config.k8s.recommendation.ContainerState;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

/**
 * Recommends ResourceRequirements (requests & limits) for a container.
 */
public interface ContainerResourceRequirementEstimator {
  ResourceRequirement getEstimatedResourceRequirements(ContainerState containerState);
}
