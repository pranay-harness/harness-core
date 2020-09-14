package io.harness.batch.processing.config.k8s.recommendation.estimators;

import io.harness.batch.processing.config.k8s.recommendation.ContainerState;
import lombok.Value;

import java.util.Map;

/**
 * Returns a constant estimation (for testing).
 */
@Value(staticConstructor = "of")
class ConstEstimator implements ResourceEstimator {
  Map<String, Long> resources;
  @Override
  public Map<String, Long> getResourceEstimation(ContainerState containerState) {
    return resources;
  }
}
