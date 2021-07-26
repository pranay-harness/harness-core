package io.harness.ccm.commons.dao.recommendation;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Optional.ofNullable;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceId;
import io.harness.ccm.commons.entities.ClusterRecord;
import io.harness.ccm.commons.entities.k8s.recommendation.K8sWorkloadRecommendation;
import io.harness.ccm.commons.service.intf.ClusterRecordService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.istack.internal.Nullable;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.NonNull;

/**
 * //TODO(UTSAV): Migrate other methods containing logic from K8sRecommendationDAO to this class,
 * so that K8sRecommendationDAO remains pure DAO class. E.g., move #getServiceProvider()
 */
@Singleton
@OwnedBy(HarnessTeam.CE)
public class RecommendationCrudServiceImpl implements RecommendationCrudService {
  @Inject private K8sRecommendationDAO k8sRecommendationDAO;
  @Inject private ClusterRecordService clusterRecordService;

  @Override
  public void upsertWorkloadRecommendation(
      @NonNull String uuid, @NonNull ResourceId workloadId, @NonNull K8sWorkloadRecommendation recommendation) {
    final String clusterName = fetchClusterName(workloadId.getClusterId());

    final Double monthlyCost = calculateMonthlyCost(recommendation);
    final Double monthlySaving =
        ofNullable(recommendation.getEstimatedSavings()).map(BigDecimal::doubleValue).orElse(null);

    k8sRecommendationDAO.upsertCeRecommendation(uuid, workloadId, clusterName, monthlyCost, monthlySaving,
        recommendation.shouldShowRecommendation(),
        firstNonNull(recommendation.getLastReceivedUtilDataAt(), Instant.EPOCH));
  }

  @Override
  public void upsertNodeRecommendation(
      String entityUuid, JobConstants jobConstants, NodePoolId nodePoolId, RecommendationOverviewStats stats) {
    final String clusterName = fetchClusterName(nodePoolId.getClusterid());

    k8sRecommendationDAO.upsertCeRecommendation(
        entityUuid, jobConstants, nodePoolId, clusterName, stats, Instant.ofEpochMilli(jobConstants.getJobEndTime()));
  }

  @Nullable
  private String fetchClusterName(String clusterId) {
    final ClusterRecord clusterRecord = clusterRecordService.get(clusterId);
    // better return clusterId than null for cg connectors, to facilitate debugging during ng migration
    return ofNullable(clusterRecord).map(ClusterRecord::getClusterName).orElse(clusterId);
  }

  @Nullable
  private static Double calculateMonthlyCost(@NonNull K8sWorkloadRecommendation recommendation) {
    if (recommendation.isLastDayCostAvailable()) {
      return BigDecimal.ZERO.add(recommendation.getLastDayCost().getCpu())
          .add(recommendation.getLastDayCost().getMemory())
          .multiply(BigDecimal.valueOf(30))
          .setScale(2, BigDecimal.ROUND_HALF_EVEN)
          .doubleValue();
    }
    return null;
  }
}
