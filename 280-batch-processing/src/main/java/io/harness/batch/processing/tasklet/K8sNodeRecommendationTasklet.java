package io.harness.batch.processing.tasklet;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.utils.TimeUtils.toOffsetDateTime;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.pricing.client.BanzaiRecommenderClient;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.beans.billing.InstanceCategory;
import io.harness.ccm.commons.beans.recommendation.K8sServiceProvider;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.TotalResourceUsage;
import io.harness.ccm.commons.beans.recommendation.models.RecommendClusterRequest;
import io.harness.ccm.commons.beans.recommendation.models.RecommendationResponse;
import io.harness.ccm.commons.dao.recommendation.K8sRecommendationDAO;
import io.harness.exception.InvalidRequestException;

import java.net.ConnectException;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import retrofit2.Response;

/**
 * This class is stateful as of now, we are not processing accounts in parallel.
 */
@Slf4j
@OwnedBy(CE)
public class K8sNodeRecommendationTasklet implements Tasklet {
  @Autowired private K8sRecommendationDAO k8sRecommendationDAO;
  @Autowired private BanzaiRecommenderClient banzaiRecommenderClient;
  @Autowired private VMPricingService vmPricingService;

  private JobConstants jobConstants;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    this.jobConstants = new CCMJobConstants(chunkContext);

    List<NodePoolId> nodePoolIdList = k8sRecommendationDAO.getUniqueNodePools(jobConstants.getAccountId());

    for (NodePoolId nodePoolId : nodePoolIdList) {
      if (nodePoolId.getNodepoolname() == null) {
        log.info("There is a node with node_pool_name as null in [accountId:{}, clusterId:{}], skipping",
            jobConstants.getAccountId(), nodePoolId.getClusterid());
        continue;
      }

      TotalResourceUsage totalResourceUsage = getTotalResourceUsageAndInsert(nodePoolId);

      logTotalResourceUsage(nodePoolId, totalResourceUsage);

      try {
        calculateAndSaveRecommendation(nodePoolId, totalResourceUsage);
      } catch (InvalidRequestException ex) {
        log.warn("Not generating recommendation for: {} with TRU: {}", nodePoolId, totalResourceUsage, ex);
      }
    }

    return null;
  }

  private TotalResourceUsage getTotalResourceUsageAndInsert(@NonNull NodePoolId nodePoolId) {
    TotalResourceUsage totalResourceUsage =
        k8sRecommendationDAO.maxResourceOfAllTimeBucketsForANodePool(jobConstants, nodePoolId);
    k8sRecommendationDAO.insertNodePoolAggregated(jobConstants, nodePoolId, totalResourceUsage);
    return totalResourceUsage;
  }

  private void logTotalResourceUsage(@NonNull NodePoolId nodePoolId, @NonNull TotalResourceUsage totalResourceUsage) {
    log.info("TotalResourceUsage for {}:{} is {} between {} and {}", jobConstants.getAccountId(), nodePoolId.toString(),
        totalResourceUsage.toString(), toOffsetDateTime(jobConstants.getJobStartTime()),
        toOffsetDateTime(jobConstants.getJobEndTime()));
  }

  private void calculateAndSaveRecommendation(
      @NonNull NodePoolId nodePoolId, @NonNull TotalResourceUsage totalResourceUsage) {
    K8sServiceProvider serviceProvider = k8sRecommendationDAO.getServiceProvider(jobConstants, nodePoolId);
    log.info("serviceProvider: {}", serviceProvider);

    RecommendationResponse recommendation = getRecommendation(serviceProvider, totalResourceUsage);
    log.info("RecommendationResponse: {}", recommendation);

    String mongoEntityId = k8sRecommendationDAO.insertNodeRecommendationResponse(
        jobConstants, nodePoolId, totalResourceUsage, serviceProvider, recommendation);

    RecommendationOverviewStats stats = getMonthlyCostAndSaving(serviceProvider, recommendation);
    log.info("The monthly stat is: {}", stats);

    k8sRecommendationDAO.updateCeRecommendation(mongoEntityId, jobConstants, nodePoolId, stats, Instant.now());
  }

  @SneakyThrows
  private RecommendationResponse getRecommendation(
      K8sServiceProvider serviceProvider, TotalResourceUsage totalResourceUsage) {
    RecommendClusterRequest request = constructRequest(totalResourceUsage);
    log.info("RecommendClusterRequest: {}", request);

    Response<RecommendationResponse> response =
        banzaiRecommenderClient
            .getRecommendation(serviceProvider.getCloudProvider().getCloudProviderName(),
                serviceProvider.getCloudProvider().getK8sService(), serviceProvider.getRegion(), request)
            .execute();

    if (!response.isSuccessful()) {
      log.error("banzaiRecommenderClient response: {}", response);
      throw new ConnectException("Failed to get/parse response from banzai recommender");
    }

    RecommendationResponse recommendation = response.body();
    recommendation.setInstanceCategory(InstanceCategory.ON_DEMAND);

    if (InstanceCategory.SPOT.equals(serviceProvider.getInstanceCategory())) {
      double totalSpotPrice =
          recommendation.getAccuracy().getMasterPrice() + recommendation.getAccuracy().getSpotPrice();
      recommendation.getAccuracy().setTotalPrice(totalSpotPrice);

      recommendation.setInstanceCategory(InstanceCategory.SPOT);
    }

    return recommendation;
  }

  private static RecommendClusterRequest constructRequest(TotalResourceUsage totalResourceUsage) {
    if (!isResourceConsistent(totalResourceUsage)) {
      throw new InvalidRequestException(String.format("Inconsistent TotalResourceUsage: %s", totalResourceUsage));
    }

    long minNodes = 3L;
    long maxNodesPossible = (long) Math.min(Math.floor(totalResourceUsage.getSumcpu() / totalResourceUsage.getMaxcpu()),
        Math.floor(totalResourceUsage.getSummemory() / totalResourceUsage.getMaxmemory()));

    maxNodesPossible = Math.max(maxNodesPossible, 1L);
    if (maxNodesPossible < 3L) {
      minNodes = maxNodesPossible;
    }

    return RecommendClusterRequest.builder()
        .maxNodes(maxNodesPossible)
        .minNodes(minNodes)
        .sumCpu(totalResourceUsage.getSumcpu() / 1024.0D)
        .sumMem(totalResourceUsage.getSummemory() / 1024.0D)
        .allowBurst(true)
        .sameSize(true)
        .build();
  }

  private RecommendationOverviewStats getMonthlyCostAndSaving(
      @NonNull K8sServiceProvider serviceProvider, @NonNull RecommendationResponse recommendation) {
    double currentPricePerVm = getCurrentInstancePrice(serviceProvider);

    double currentHourlyCost = currentPricePerVm * (double) serviceProvider.getNodeCount();
    double recommendedHourlyCost = recommendation.getAccuracy().getTotalPrice();

    final double toMonthly = 24 * 30;
    return RecommendationOverviewStats.builder()
        .totalMonthlyCost(currentHourlyCost * toMonthly)
        .totalMonthlySaving((currentHourlyCost - recommendedHourlyCost) * toMonthly)
        .build();
  }

  private double getCurrentInstancePrice(@NonNull K8sServiceProvider serviceProvider) {
    if (serviceProvider.getInstanceFamily() == null) {
      log.warn("Incomplete K8sServiceProvider {}, returning currentCost = 0", serviceProvider);
      return 0; // shall we use any default value?
    }

    VMComputePricingInfo vmComputePricingInfo = vmPricingService.getComputeVMPricingInfo(
        serviceProvider.getInstanceFamily(), serviceProvider.getRegion(), serviceProvider.getCloudProvider());

    log.info("Current Pricing {}", vmComputePricingInfo);
    if (vmComputePricingInfo == null) {
      log.error("Current Pricing not available for {}", serviceProvider);
      throw new InvalidRequestException("Pricing not available, not saving recommendation in timescaleDB");
    }

    if (InstanceCategory.SPOT.equals(serviceProvider.getInstanceCategory())
        && !isEmpty(vmComputePricingInfo.getSpotPrice())) {
      // get any zone price, generally they all are same
      return vmComputePricingInfo.getSpotPrice().get(0).getPrice();
    }

    return vmComputePricingInfo.getOnDemandPrice();
  }

  private static boolean isResourceConsistent(@NonNull TotalResourceUsage resource) {
    boolean inconsistent = Math.round(resource.getSumcpu()) < Math.round(resource.getMaxcpu())
        || Math.round(resource.getSummemory()) < Math.round(resource.getMaxmemory());
    boolean anyZero = Math.round(resource.getSumcpu()) == 0L || Math.round(resource.getSummemory()) == 0L
        || Math.round(resource.getMaxcpu()) == 0L || Math.round(resource.getMaxmemory()) == 0L;

    return !inconsistent && !anyZero;
  }
}
