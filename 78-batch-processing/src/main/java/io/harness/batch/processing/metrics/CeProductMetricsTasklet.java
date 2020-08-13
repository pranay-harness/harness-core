package io.harness.batch.processing.metrics;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import com.segment.analytics.Analytics;
import com.segment.analytics.messages.GroupMessage;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.event.handler.segment.SegmentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.Account;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Singleton
public class CeProductMetricsTasklet implements Tasklet {
  @Autowired private BatchMainConfig mainConfiguration;
  @Autowired private ProductMetricsService productMetricsService;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private CeCloudMetricsService ceCloudMetricsService;
  private JobParameters parameters;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    if (mainConfiguration.getSegmentConfig().isEnabled()) {
      parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
      String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);

      Instant start = CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE)
                          .minus(3, ChronoUnit.DAYS);
      Instant end = CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE)
                        .minus(3, ChronoUnit.DAYS);
      logger.info("Sending CE account traits through Segment group call.");
      sendStatsToSegment(accountId, start, end);
    }
    return null;
  }

  public void sendStatsToSegment(String accountId, Instant start, Instant end) {
    Account account = cloudToHarnessMappingService.getAccountInfoFromId(accountId);
    SegmentConfig segmentConfig = mainConfiguration.getSegmentConfig();
    String writeKey = segmentConfig.getApiKey();
    Analytics analytics = Analytics.builder(writeKey).build();
    ImmutableMap.Builder<String, Object> groupTraitsMapBuilder =
        ImmutableMap.<String, Object>builder()
            .put("is_ce_enabled", account.isCloudCostEnabled())
            .put("company_name", account.getCompanyName())
            .put("total_aws_cloud_cost", ceCloudMetricsService.getTotalCloudCost(accountId, "AWS", start, end))
            .put("total_gcp_cloud_cost", ceCloudMetricsService.getTotalCloudCost(accountId, "GCP", start, end))
            .put("total_cluster_cost", productMetricsService.getTotalClusterCost(accountId, start, end))
            .put("total_unallocated_cost", productMetricsService.getTotalUnallocatedCost(accountId, start, end))
            .put("total_idle_cost", productMetricsService.getTotalIdleCost(accountId, start, end))
            .put("overall_unallocated_cost_percentage",
                productMetricsService.getOverallUnallocatedCostPercentage(accountId, start, end))
            .put("overall_idle_cost_percentage",
                productMetricsService.getOverallIdleCostPercentage(accountId, start, end))

            .put("total_k8s_spend_in_ce", productMetricsService.getTotalK8sSpendInCe(accountId, start, end))
            .put("total_ecs_spend_in_ce", productMetricsService.getTotalEcsSpendInCe(accountId, start, end))
            .put("num_gcp_billing_accounts", productMetricsService.countGcpBillingAccounts(accountId))
            .put("num_aws_billing_accounts", productMetricsService.countAwsBillingAccounts(accountId))
            .put("total_aws_cloud_provider_in_cd", productMetricsService.countAwsCloudProviderInCd(accountId))
            .put("total_aws_cloud_provider_in_ce", productMetricsService.countAwsCloudProviderInCe(accountId))
            .put("total_k8s_clusters_in_cd", productMetricsService.countK8sClusterInCd(accountId))
            .put("total_k8s_clusters_in_ce", productMetricsService.countK8sClusterInCe(accountId))
            .put("total_k8s_namespaces", productMetricsService.countTotalK8sNamespaces(accountId, start, end))
            .put("total_k8s_workloads", productMetricsService.countTotalK8sWorkloads(accountId, start, end))
            .put("total_k8s_nodes", productMetricsService.countTotalK8sNodes(accountId, start, end))
            .put("total_k8s_pods", productMetricsService.countTotalK8sPods(accountId, start, end))

            .put("total_ecs_clusters", productMetricsService.countTotalEcsClusters(accountId, start, end))
            .put("total_ecs_tasks", productMetricsService.countTotalEcsTasks(accountId, start, end));

    CeLicenseInfo ceLicenseInfo =
        Optional.ofNullable(account.getCeLicenseInfo()).orElse(CeLicenseInfo.builder().build());
    if (ceLicenseInfo.getLicenseType() != null) {
      groupTraitsMapBuilder.put("ce_license_type", ceLicenseInfo.getLicenseType().name());
    }
    groupTraitsMapBuilder.put("ce_license_expiry", ceLicenseInfo.getExpiryTime());

    analytics.enqueue(GroupMessage.builder(accountId)
                          .anonymousId(accountId)
                          .timestamp(Date.from(end))
                          .traits(groupTraitsMapBuilder.build()));
    logger.info("Sent CE account traits through Segment group call.");
  }
}
