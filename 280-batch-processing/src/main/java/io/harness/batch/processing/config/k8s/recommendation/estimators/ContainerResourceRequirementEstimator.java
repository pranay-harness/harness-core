package io.harness.batch.processing.config.k8s.recommendation.estimators;

import io.harness.batch.processing.config.k8s.recommendation.ContainerState;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

/**
 * Recommends ResourceRequirements (requests & limits) for a container.
 */
public interface ContainerResourceRequirementEstimator {
  ResourceRequirement getEstimatedResourceRequirements(ContainerState containerState);
}
