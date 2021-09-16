package io.harness.ccm.commons.dao.recommendation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceId;
import io.harness.ccm.commons.entities.k8s.recommendation.K8sWorkloadRecommendation;

import lombok.NonNull;

@OwnedBy(HarnessTeam.CE)
public interface RecommendationCrudService {
  void upsertWorkloadRecommendation(@NonNull String uuid, @NonNull ResourceId workloadId, @NonNull String clusterName,
      @NonNull K8sWorkloadRecommendation recommendation);

  void upsertNodeRecommendation(String entityUuid, @NonNull JobConstants jobConstants, @NonNull NodePoolId nodePoolId,
      @NonNull String clusterName, @NonNull RecommendationOverviewStats stats);
}
