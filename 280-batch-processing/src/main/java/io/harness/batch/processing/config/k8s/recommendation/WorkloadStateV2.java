package io.harness.batch.processing.config.k8s.recommendation;

import io.harness.ccm.commons.entities.k8s.recommendation.PartialRecommendationHistogram;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Value;

@Value
class WorkloadStateV2 {
  PartialRecommendationHistogram histogram;
  Map<String, ContainerStateV2> containerStateMap;

  WorkloadStateV2(PartialRecommendationHistogram partialRecommendationHistogram) {
    this.histogram = partialRecommendationHistogram;
    this.containerStateMap =
        Optional.ofNullable(partialRecommendationHistogram.getContainerCheckpoints())
            .orElseGet(HashMap::new)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> ContainerStateV2.fromCheckpoint(e.getValue())));
  }
}
