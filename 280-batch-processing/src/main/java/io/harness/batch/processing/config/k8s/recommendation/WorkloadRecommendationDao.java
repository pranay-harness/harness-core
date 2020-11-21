package io.harness.batch.processing.config.k8s.recommendation;

import io.harness.persistence.HPersistence;

import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation.K8sWorkloadRecommendationKeys;

import java.util.HashMap;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Repository;

@Repository
public class WorkloadRecommendationDao {
  private final HPersistence hPersistence;

  public WorkloadRecommendationDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  @NotNull
  K8sWorkloadRecommendation fetchRecommendationForWorkload(ResourceId workloadId) {
    return Optional
        .ofNullable(hPersistence.createQuery(K8sWorkloadRecommendation.class)
                        .field(K8sWorkloadRecommendationKeys.accountId)
                        .equal(workloadId.getAccountId())
                        .field(K8sWorkloadRecommendationKeys.clusterId)
                        .equal(workloadId.getClusterId())
                        .field(K8sWorkloadRecommendationKeys.namespace)
                        .equal(workloadId.getNamespace())
                        .field(K8sWorkloadRecommendationKeys.workloadName)
                        .equal(workloadId.getName())
                        .field(K8sWorkloadRecommendationKeys.workloadType)
                        .equal(workloadId.getKind())
                        .get())
        .orElseGet(()
                       -> K8sWorkloadRecommendation.builder()
                              .accountId(workloadId.getAccountId())
                              .clusterId(workloadId.getClusterId())
                              .namespace(workloadId.getNamespace())
                              .workloadName(workloadId.getName())
                              .workloadType(workloadId.getKind())
                              .containerRecommendations(new HashMap<>())
                              .containerCheckpoints(new HashMap<>())
                              .build());
  }

  void save(K8sWorkloadRecommendation recommendation) {
    hPersistence.save(recommendation);
  }
}
