package io.harness.batch.processing.config.k8s.recommendation;

import io.harness.ccm.commons.entities.k8s.recommendation.K8sWorkloadRecommendation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Value;

@Value
class WorkloadState {
  K8sWorkloadRecommendation recommendation;
  Map<String, ContainerState> containerStateMap;

  WorkloadState(K8sWorkloadRecommendation recommendation) {
    this.recommendation = recommendation;
    this.containerStateMap =
        Optional.ofNullable(recommendation.getContainerCheckpoints())
            .orElseGet(HashMap::new)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> ContainerState.fromCheckpoint(e.getValue())));
  }
}
