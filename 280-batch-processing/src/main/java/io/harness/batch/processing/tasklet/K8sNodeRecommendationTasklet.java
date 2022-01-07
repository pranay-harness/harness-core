/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.tasklet;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.utils.TimeUtils.toOffsetDateTime;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.pricing.banzai.BanzaiRecommenderClient;
import io.harness.batch.processing.pricing.vmpricing.VMPricingService;
import io.harness.batch.processing.tasklet.util.ClusterHelper;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.beans.billing.InstanceCategory;
import io.harness.ccm.commons.beans.recommendation.K8sServiceProvider;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.RecommendationUtils;
import io.harness.ccm.commons.beans.recommendation.TotalResourceUsage;
import io.harness.ccm.commons.beans.recommendation.models.RecommendClusterRequest;
import io.harness.ccm.commons.beans.recommendation.models.RecommendationResponse;
import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.ccm.commons.dao.recommendation.K8sRecommendationDAO;
import io.harness.ccm.commons.dao.recommendation.RecommendationCrudService;
import io.harness.exception.InvalidRequestException;
import io.harness.pricing.dto.cloudinfo.ProductDetails;

import java.net.ConnectException;
import java.util.List;
import javax.validation.constraints.NotNull;
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
  @Autowired private RecommendationCrudService recommendationCrudService;
  @Autowired private BanzaiRecommenderClient banzaiRecommenderClient;
  @Autowired private VMPricingService vmPricingService;
  @Autowired private ClusterHelper clusterHelper;

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
      } catch (InvalidRequestException
              ex) { // This an acceptable exception in the sense that the next recommendation job should proceed.
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
    K8sServiceProvider serviceProvider = getCurrentNodePoolConfiguration(nodePoolId);
    log.info("serviceProvider: {}", serviceProvider);

    RecommendClusterRequest request = RecommendationUtils.constructNodeRecommendationRequest(totalResourceUsage);
    log.info("RecommendClusterRequest: {}", request);

    RecommendationResponse recommendation = getRecommendation(serviceProvider, request);
    log.info("RecommendationResponse: {}", recommendation);

    String mongoEntityId = k8sRecommendationDAO.insertNodeRecommendationResponse(
        jobConstants, nodePoolId, request, serviceProvider, recommendation);

    RecommendationOverviewStats stats = getMonthlyCostAndSaving(serviceProvider, recommendation);
    log.info("The monthly stat is: {}", stats);

    final String clusterName = clusterHelper.fetchClusterName(nodePoolId.getClusterid());
    recommendationCrudService.upsertNodeRecommendation(mongoEntityId, jobConstants, nodePoolId, clusterName, stats);
  }

  private K8sServiceProvider getCurrentNodePoolConfiguration(@NonNull NodePoolId nodePoolId) {
    K8sServiceProvider serviceProvider = k8sRecommendationDAO.getServiceProvider(jobConstants, nodePoolId);

    populateVMInfo(serviceProvider);

    return serviceProvider;
  }

  @SneakyThrows
  private RecommendationResponse getRecommendation(
      K8sServiceProvider serviceProvider, RecommendClusterRequest request) {
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

  @NotNull
  private RecommendationOverviewStats getMonthlyCostAndSaving(
      @NonNull K8sServiceProvider serviceProvider, @NonNull RecommendationResponse recommendation) {
    double currentHourlyCost = serviceProvider.getCategoryAwareCost() * (double) serviceProvider.getNodeCount();
    double recommendedHourlyCost = recommendation.getAccuracy().getTotalPrice();

    final double toMonthly = 24 * 30;
    return RecommendationOverviewStats.builder()
        .totalMonthlyCost(currentHourlyCost * toMonthly)
        .totalMonthlySaving((currentHourlyCost - recommendedHourlyCost) * toMonthly)
        .build();
  }

  private void populateVMInfo(@NonNull K8sServiceProvider serviceProvider) {
    if (serviceProvider.getInstanceFamily() == null) {
      log.warn("Incomplete K8sServiceProvider {}, returning currentCost = 0", serviceProvider);
      return; // shall we use any default value?
    }

    String supportedRegion = serviceProvider.getRegion();
    if (serviceProvider.getCloudProvider() == CloudProvider.AZURE) {
      supportedRegion = VMPricingService.getSimilarRegionIfNotSupportedByBanzai(serviceProvider.getRegion());
    }

    serviceProvider.setRegion(supportedRegion);

    ProductDetails vmComputePricingInfo = vmPricingService.getComputeVMPricingInfo(
        serviceProvider.getInstanceFamily(), serviceProvider.getRegion(), serviceProvider.getCloudProvider());

    log.info("Current Pricing {}", vmComputePricingInfo);
    if (vmComputePricingInfo == null) {
      log.error("Current Pricing not available for {}", serviceProvider);
      return;
    }

    serviceProvider.setCpusPerVm(vmComputePricingInfo.getCpusPerVm());
    serviceProvider.setMemPerVm(vmComputePricingInfo.getMemPerVm());

    serviceProvider.setCostPerVmPerHr(vmComputePricingInfo.getOnDemandPrice());
    if (!isEmpty(vmComputePricingInfo.getSpotPrice())) {
      serviceProvider.setSpotCostPerVmPerHr(vmComputePricingInfo.getSpotPrice().get(0).getPrice());
    }
  }
}
