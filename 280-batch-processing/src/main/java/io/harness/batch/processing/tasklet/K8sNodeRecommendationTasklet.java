package io.harness.batch.processing.tasklet;

import static io.harness.ccm.commons.Constants.ZONE_OFFSET;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.TotalResourceUsage;
import io.harness.ccm.commons.dao.recommendation.K8sRecommendationDAO;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class K8sNodeRecommendationTasklet implements Tasklet {
  @Autowired private K8sRecommendationDAO k8sRecommendationDAO;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    final JobConstants jobConstants = new CCMJobConstants(chunkContext);

    List<NodePoolId> nodePoolIdList = k8sRecommendationDAO.getUniqueNodePools(jobConstants.getAccountId());

    for (NodePoolId nodePoolId : nodePoolIdList) {
      TotalResourceUsage totalResourceUsage =
          k8sRecommendationDAO.maxResourceOfAllTimeBucketsForANodePool(jobConstants, nodePoolId);
      k8sRecommendationDAO.insertNodePoolAggregated(jobConstants, nodePoolId, totalResourceUsage);

      logTotalResourceUsage(jobConstants, nodePoolId, totalResourceUsage);
    }

    return null;
  }

  private void logTotalResourceUsage(@NonNull JobConstants jobConstants, @NonNull NodePoolId nodePoolId,
      @NonNull TotalResourceUsage totalResourceUsage) {
    log.info("TotalResourceUsage for {}:{} is {} between {} and {}", jobConstants.getAccountId(), nodePoolId.toString(),
        totalResourceUsage.toString(),
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(jobConstants.getJobStartTime()), ZONE_OFFSET),
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(jobConstants.getJobEndTime()), ZONE_OFFSET));
  }
}
