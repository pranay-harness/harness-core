package io.harness.batch.processing.config.k8s.recommendation;

import static java.util.Collections.emptyMap;

import com.google.common.collect.ImmutableSet;

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

  static Map<String, String> sanitized(Map<String, String> resourceMap) {
    ImmutableSet<String> trackedResources = ImmutableSet.of("cpu", "memory");
    return Optional.ofNullable(resourceMap)
        .orElse(emptyMap())
        .entrySet()
        .stream()
        .filter(resourceEntry -> { return trackedResources.contains(resourceEntry.getKey()); })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

      // Make sure the list of containers in the recommendation match the list of containers in the spec
      List<K8sWorkloadSpec.ContainerSpec> containerSpecs = k8sWorkloadSpec.getContainerSpecsList();
      Map<String, ContainerRecommendation> containerRecommendations =
          containerSpecs.stream().collect(Collectors.toMap(K8sWorkloadSpec.ContainerSpec::getName, e -> {
            Map<String, String> requestsMap = new HashMap<>(sanitized(e.getRequestsMap()));
            Map<String, String> limitsMap = sanitized(e.getLimitsMap());
            limitsMap.forEach(requestsMap::putIfAbsent);
            return ContainerRecommendation.builder()
                .containerName(e.getName())
                .current(ResourceRequirement.builder().requests(requestsMap).limits(limitsMap).build())
                .build();
          }));
      workloadToRecommendation.computeIfAbsent(workloadId, workloadRecommendationDao::fetchRecommendationForWorkload)
          .setContainerRecommendations(containerRecommendations);
    }
    updateRecommendations();
  }

  private void updateRecommendations() {
    workloadToRecommendation.values().forEach(workloadRecommendationDao::save);
    workloadToRecommendation.clear();
  }
}
