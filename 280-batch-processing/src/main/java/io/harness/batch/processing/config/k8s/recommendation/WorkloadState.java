package io.harness.batch.processing.config.k8s.recommendation;

import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Value;

@Value
class WorkloadState {
  Map<String, ContainerState> containerStateMap;
  Map<String, ContainerStateV2> containerStateMapSingleDay;

  WorkloadState(K8sWorkloadRecommendation recommendation) {
    this.containerStateMap =
        Optional.ofNullable(recommendation.getContainerCheckpoints())
            .orElseGet(HashMap::new)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> ContainerState.fromCheckpoint(e.getValue())));

    this.containerStateMapSingleDay = Optional.ofNullable(recommendation.getContainerCheckpoints())
                                          .orElseGet(HashMap::new)
                                          .entrySet()
                                          .stream()
                                          .collect(Collectors.toMap(Map.Entry::getKey, e -> new ContainerStateV2()));
  }
}
