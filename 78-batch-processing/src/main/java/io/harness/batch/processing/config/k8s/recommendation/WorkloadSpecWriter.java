package io.harness.batch.processing.config.k8s.recommendation;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import io.harness.event.grpc.PublishedMessage;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
class WorkloadSpecWriter implements ItemWriter<PublishedMessage> {
  private final WorkloadRecommendationDao workloadRecommendationDao;

  // account level cache
  private final Map<ResourceId, K8sWorkloadRecommendation> workloadToRecommendation;

  WorkloadSpecWriter(WorkloadRecommendationDao workloadRecommendationDao) {
    this.workloadRecommendationDao = workloadRecommendationDao;
    this.workloadToRecommendation = new HashMap<>();
  }

  @Override
  public void write(List<? extends PublishedMessage> items) throws Exception {
    for (PublishedMessage item : items) {
      String accountId = item.getAccountId();
      K8sWorkloadSpec k8sWorkloadSpec = (K8sWorkloadSpec) item.getMessage();
      ResourceId workloadId = ResourceId.builder()
                                  .accountId(accountId)
                                  .clusterId(k8sWorkloadSpec.getClusterId())
                                  .namespace(k8sWorkloadSpec.getNamespace())
                                  .name(k8sWorkloadSpec.getWorkloadName())
                                  .kind(k8sWorkloadSpec.getWorkloadKind())
                                  .build();

      List<K8sWorkloadSpec.ContainerSpec> containerSpecs = k8sWorkloadSpec.getContainerSpecsList();
      Map<String, ResourceRequirement> containerCurrentResources =
          containerSpecs.stream().collect(Collectors.toMap(K8sWorkloadSpec.ContainerSpec::getName, e -> {
            Map<String, String> requestsMap = new HashMap<>(ofNullable(e.getRequestsMap()).orElse(emptyMap()));
            Map<String, String> limitsMap = ofNullable(e.getLimitsMap()).orElse(emptyMap());
            limitsMap.forEach(requestsMap::putIfAbsent);
            return ResourceRequirement.builder().requests(requestsMap).limits(limitsMap).build();
          }));

      K8sWorkloadRecommendation recommendation = workloadToRecommendation.computeIfAbsent(
          workloadId, workloadRecommendationDao::fetchRecommendationForWorkload);

      Map<String, ContainerRecommendation> existingRecommendations = recommendation.getContainerRecommendations();
      Map<String, ContainerRecommendation> updatedRecommendations =
          containerCurrentResources.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
              currentResources
              -> ContainerRecommendation.builder()
                     .current(currentResources.getValue())
                     .guaranteed(Optional.ofNullable(existingRecommendations.get(currentResources.getKey()))
                                     .map(ContainerRecommendation::getGuaranteed)
                                     .orElse(null))
                     .burstable(Optional.ofNullable(existingRecommendations.get(currentResources.getKey()))
                                    .map(ContainerRecommendation::getBurstable)
                                    .orElse(null))
                     .build()));
      recommendation.setContainerRecommendations(updatedRecommendations);
    }
    updateRecommendations();
  }

  private void updateRecommendations() {
    workloadToRecommendation.values().forEach(workloadRecommendationDao::save);
    workloadToRecommendation.clear();
  }
}
