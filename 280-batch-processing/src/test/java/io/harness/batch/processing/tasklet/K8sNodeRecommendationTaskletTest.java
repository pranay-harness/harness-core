/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.tasklet;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.batch.processing.pricing.banzai.BanzaiRecommenderClient;
import io.harness.batch.processing.pricing.banzai.VMComputePricingInfo;
import io.harness.batch.processing.pricing.banzai.ZonePrice;
import io.harness.batch.processing.pricing.vmpricing.VMPricingService;
import io.harness.batch.processing.tasklet.util.ClusterHelper;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.billing.InstanceCategory;
import io.harness.ccm.commons.beans.recommendation.K8sServiceProvider;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.TotalResourceUsage;
import io.harness.ccm.commons.beans.recommendation.models.RecommendClusterRequest;
import io.harness.ccm.commons.beans.recommendation.models.RecommendationResponse;
import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.ccm.commons.dao.recommendation.K8sRecommendationDAO;
import io.harness.ccm.commons.dao.recommendation.RecommendationCrudService;
import io.harness.rule.Owner;
import io.harness.testsupport.BaseTaskletTest;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import retrofit2.Response;

@RunWith(MockitoJUnitRunner.class)
public class K8sNodeRecommendationTaskletTest extends BaseTaskletTest {
  @Mock private K8sRecommendationDAO k8sRecommendationDAO;
  @Mock private VMPricingService vmPricingService;
  @Mock private RecommendationCrudService recommendationCrudService;
  @Mock private ClusterHelper clusterHelper;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private BanzaiRecommenderClient banzaiRecommenderClient;
  @InjectMocks private K8sNodeRecommendationTasklet tasklet;

  private static final String NODE_POOL_NAME = "nodePoolName";
  private static final String CLUSTER_NAME = "clusterName";
  private static final String CLUSTER_ID = "clusterId";
  private static final Gson GSON = new Gson();

  private K8sServiceProvider k8sServiceProvider;
  private NodePoolId nodePoolId;

  @Before
  public void setUp() throws Exception {
    TotalResourceUsage resourceUsage =
        TotalResourceUsage.builder().maxcpu(1).maxmemory(3).sumcpu(8).summemory(64).build();
    RecommendClusterRequest request =
        RecommendClusterRequest.builder().sumCpu(8D).sumMem(3D).maxNodes(7L).minNodes(3L).build();

    nodePoolId = NodePoolId.builder().clusterid(CLUSTER_ID).nodepoolname(NODE_POOL_NAME).build();
    k8sServiceProvider = K8sServiceProvider.builder()
                             .cloudProvider(CloudProvider.GCP)
                             .instanceCategory(InstanceCategory.ON_DEMAND)
                             .nodeCount(6)
                             .region("us-west-1")
                             .instanceFamily("xyz")
                             .build();
    VMComputePricingInfo pricingInfo =
        VMComputePricingInfo.builder()
            .onDemandPrice(2D)
            .cpusPerVm(4D)
            .memPerVm(64D)
            .spotPrice(Collections.singletonList(ZonePrice.builder().price(1D).zone("a").build()))
            .build();

    // #execute
    when(k8sRecommendationDAO.getUniqueNodePools(eq(ACCOUNT_ID))).thenReturn(ImmutableList.of(nodePoolId));

    // #getTotalResourceUsageAndInsert
    when(k8sRecommendationDAO.maxResourceOfAllTimeBucketsForANodePool(any(), eq(nodePoolId))).thenReturn(resourceUsage);
    doNothing().when(k8sRecommendationDAO).insertNodePoolAggregated(any(), eq(nodePoolId), eq(resourceUsage));

    // #getRecommendation
    when(banzaiRecommenderClient
             .getRecommendation(eq(k8sServiceProvider.getCloudProvider().getCloudProviderName()),
                 eq(k8sServiceProvider.getCloudProvider().getK8sService()), eq(k8sServiceProvider.getRegion()), any())
             .execute())
        .thenReturn(Response.success(getRecommendationResponse()));

    // #getMonthlyCostAndSaving
    when(vmPricingService.getComputeVMPricingInfo(eq(k8sServiceProvider.getInstanceFamily()),
             eq(k8sServiceProvider.getRegion()), eq(k8sServiceProvider.getCloudProvider())))
        .thenReturn(pricingInfo);

    // #calculateAndSaveRecommendation
    String entityUuid = "entityUuid";
    when(k8sRecommendationDAO.getServiceProvider(any(), eq(nodePoolId))).thenReturn(k8sServiceProvider);
    when(clusterHelper.fetchClusterName(eq(CLUSTER_ID))).thenReturn(CLUSTER_NAME);
    when(k8sRecommendationDAO.insertNodeRecommendationResponse(
             any(), eq(nodePoolId), eq(request), eq(k8sServiceProvider), eq(getRecommendationResponse())))
        .thenReturn(entityUuid);
    doNothing()
        .when(recommendationCrudService)
        .upsertNodeRecommendation(eq(entityUuid), any(), eq(nodePoolId), eq(CLUSTER_NAME), any());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testExecuteSuccess() throws Exception {
    assertThat(tasklet.execute(null, chunkContext)).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testExecuteNoNodePools() throws Exception {
    when(k8sRecommendationDAO.getUniqueNodePools(any())).thenReturn(Collections.emptyList());
    assertThat(tasklet.execute(null, chunkContext)).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testExecuteWithNullNodePoolName() throws Exception {
    NodePoolId nodePoolId = NodePoolId.builder().clusterid(CLUSTER_ID).nodepoolname(null).build();

    when(k8sRecommendationDAO.getUniqueNodePools(any())).thenReturn(Collections.singletonList(nodePoolId));

    assertThat(tasklet.execute(null, chunkContext)).isNull();

    verify(k8sRecommendationDAO, times(0)).maxResourceOfAllTimeBucketsForANodePool(any(), any());
    verify(k8sRecommendationDAO, times(0)).insertNodePoolAggregated(any(), any(), any());
    // first time it's invoked at K8sNodeRecommendationTaskletTest#setUp, due to deep stub probably, so effectively 0
    // execution.
    verify(banzaiRecommenderClient, times(1)).getRecommendation(any(), any(), any(), any());
    verify(k8sRecommendationDAO, times(0)).insertNodeRecommendationResponse(any(), any(), any(), any(), any());
    verify(recommendationCrudService, times(0)).upsertNodeRecommendation(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testWithInconsistentTotalResourceUsage_Inconsistent() throws Exception {
    TotalResourceUsage totalResourceUsage =
        TotalResourceUsage.builder().sumcpu(16D).summemory(64D).maxcpu(20D).maxmemory(4.1D).build();

    when(k8sRecommendationDAO.maxResourceOfAllTimeBucketsForANodePool(any(), any())).thenReturn(totalResourceUsage);

    // job was successful but the recommendtion was not generated and skipped
    assertThat(tasklet.execute(null, chunkContext)).isNull();

    // effectively 0 times
    verify(banzaiRecommenderClient, times(1)).getRecommendation(any(), any(), any(), any());
    verify(k8sRecommendationDAO, times(0)).insertNodeRecommendationResponse(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testJobSuccessOnCurrentPricingInfoNotAvailable() throws Exception {
    when(vmPricingService.getComputeVMPricingInfo(eq(k8sServiceProvider.getInstanceFamily()),
             eq(k8sServiceProvider.getRegion()), eq(k8sServiceProvider.getCloudProvider())))
        .thenReturn(null);

    // job was successful but the recommendation was not generated and skipped
    assertThat(tasklet.execute(null, chunkContext)).isNull();

    // effectively 1 times
    verify(banzaiRecommenderClient, times(2)).getRecommendation(any(), any(), any(), any());
    // detailed recommendation saved in mongo DB
    verify(k8sRecommendationDAO, times(1)).insertNodeRecommendationResponse(any(), any(), any(), any(), any());
    // savings stats as 0 in timescaleDB
    ArgumentCaptor<RecommendationOverviewStats> statsCaptor =
        ArgumentCaptor.forClass(RecommendationOverviewStats.class);
    verify(recommendationCrudService, times(1))
        .upsertNodeRecommendation(any(), any(), any(), any(), statsCaptor.capture());

    final RecommendationOverviewStats stats = statsCaptor.getValue();
    assertThat(stats).isNotNull();
    assertThat(stats.getTotalMonthlyCost()).isEqualTo(0D);
    assertThat(stats.getTotalMonthlySaving()).isNotZero();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testWithInconsistentTotalResourceUsage_zeroResource() throws Exception {
    TotalResourceUsage totalResourceUsage =
        TotalResourceUsage.builder().sumcpu(0D).summemory(64D).maxcpu(20D).maxmemory(4.1D).build();

    when(k8sRecommendationDAO.maxResourceOfAllTimeBucketsForANodePool(any(), any())).thenReturn(totalResourceUsage);

    // job was successful but the recommendtion was not generated and skipped
    assertThat(tasklet.execute(null, chunkContext)).isNull();

    // effectively 0 times
    verify(banzaiRecommenderClient, times(1)).getRecommendation(any(), any(), any(), any());
    verify(k8sRecommendationDAO, times(0)).insertNodeRecommendationResponse(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testCorrectNumNodesInRequest() throws Exception {
    TotalResourceUsage totalResourceUsage =
        TotalResourceUsage.builder().sumcpu(16D).summemory(64D).maxcpu(0.9D).maxmemory(4.1D).build();

    RecommendClusterRequest request = captureRequest(totalResourceUsage);

    assertThat(request).isNotNull();
    assertThat(request.getMinNodes()).isEqualTo(3L);
    // CPU wise: 16.0/0.9 = 17.777 floor-> 17, Memory wise: 64.0/4.1 = 15.61 floor-> 15
    // Satisfy both: min(17, 15) = 15
    assertThat(request.getMaxNodes()).isEqualTo(15L);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testRequestHasAtLeastOneNode() throws Exception {
    TotalResourceUsage totalResourceUsage =
        TotalResourceUsage.builder().sumcpu(16D).summemory(64D).maxcpu(8.1D).maxmemory(4.1D).build();

    RecommendClusterRequest request = captureRequest(totalResourceUsage);

    assertThat(request).isNotNull();
    assertThat(request.getMinNodes()).isEqualTo(1L);
    // CPU wise: 16.0/8.1 = 1.975 floor-> 1, Memory wise: 64.0/4.1 = 15.61 floor-> 15
    // Satisfy both: min(1, 15) = 1
    assertThat(request.getMaxNodes()).isEqualTo(1L);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testCalculateCostAndSaving() throws Exception {
    assertThat(tasklet.execute(null, chunkContext)).isNull();

    ArgumentCaptor<RecommendationOverviewStats> captor = ArgumentCaptor.forClass(RecommendationOverviewStats.class);

    verify(recommendationCrudService, times(1)).upsertNodeRecommendation(any(), any(), any(), any(), captor.capture());

    RecommendationOverviewStats stats = captor.getValue();
    assertThat(stats).isNotNull();
    // $2 per node * 6 nodes * 24 hrs * 30 days
    assertThat(stats.getTotalMonthlyCost()).isCloseTo(2D * 6 * 24 * 30, offset(0.5D));
    // monthlyCost - $1.329993 per vm * 24 hrs * 30 days
    assertThat(stats.getTotalMonthlySaving()).isCloseTo((2D * 6 - 1.329993D) * 24 * 30, offset(0.5D));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testTotalPriceIsCorrectInSpotRecommendation() throws Exception {
    k8sServiceProvider.setInstanceCategory(InstanceCategory.SPOT);

    when(k8sRecommendationDAO.getServiceProvider(any(), eq(nodePoolId))).thenReturn(k8sServiceProvider);

    assertThat(tasklet.execute(null, chunkContext)).isNull();

    ArgumentCaptor<K8sServiceProvider> serviceProviderCaptor = ArgumentCaptor.forClass(K8sServiceProvider.class);
    ArgumentCaptor<RecommendationResponse> captor = ArgumentCaptor.forClass(RecommendationResponse.class);

    verify(k8sRecommendationDAO, times(1))
        .insertNodeRecommendationResponse(any(), any(), any(), serviceProviderCaptor.capture(), captor.capture());

    RecommendationResponse recommendation = captor.getValue();

    assertThat(recommendation).isNotNull();
    assertThat(recommendation.getInstanceCategory()).isEqualTo(InstanceCategory.SPOT);
    assertThat(recommendation.getAccuracy().getTotalPrice()).isCloseTo(0.28D + 0, offset(0.05D));
    assertThat(recommendation.getAccuracy().getTotalPrice())
        .isCloseTo(
            recommendation.getAccuracy().getSpotPrice() + recommendation.getAccuracy().getMasterPrice(), offset(0.05D));

    K8sServiceProvider serviceProvider = serviceProviderCaptor.getValue();

    assertThat(serviceProvider).isNotNull();
    assertThat(serviceProvider.getSpotCostPerVmPerHr()).isEqualTo(1D);
    assertThat(serviceProvider.getCategoryAwareCost()).isEqualTo(1D);
    assertThat(serviceProvider.getCpusPerVm()).isEqualTo(4D);
    assertThat(serviceProvider.getMemPerVm()).isEqualTo(64D);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testTotalPriceIsCorrectInOnDemandRecommendation() throws Exception {
    assertThat(tasklet.execute(null, chunkContext)).isNull();

    ArgumentCaptor<K8sServiceProvider> serviceProviderCaptor = ArgumentCaptor.forClass(K8sServiceProvider.class);
    ArgumentCaptor<RecommendationResponse> recommendationCaptor = ArgumentCaptor.forClass(RecommendationResponse.class);

    verify(k8sRecommendationDAO, times(1))
        .insertNodeRecommendationResponse(
            any(), any(), any(), serviceProviderCaptor.capture(), recommendationCaptor.capture());

    RecommendationResponse recommendation = recommendationCaptor.getValue();

    assertThat(recommendation).isNotNull();
    assertThat(recommendation.getInstanceCategory()).isEqualTo(InstanceCategory.ON_DEMAND);
    assertThat(recommendation.getAccuracy().getTotalPrice()).isCloseTo(1.329993D, offset(0.05D));
    assertThat(recommendation.getAccuracy().getTotalPrice())
        .isCloseTo(recommendation.getAccuracy().getWorkerPrice() + recommendation.getAccuracy().getMasterPrice(),
            offset(0.05D));

    K8sServiceProvider serviceProvider = serviceProviderCaptor.getValue();

    assertThat(serviceProvider).isNotNull();
    assertThat(serviceProvider.getCostPerVmPerHr()).isEqualTo(2D);
    assertThat(serviceProvider.getCategoryAwareCost()).isEqualTo(2D);
    assertThat(serviceProvider.getCpusPerVm()).isEqualTo(4D);
    assertThat(serviceProvider.getMemPerVm()).isEqualTo(64D);
  }

  private RecommendClusterRequest captureRequest(TotalResourceUsage totalResourceUsage) throws Exception {
    when(k8sRecommendationDAO.maxResourceOfAllTimeBucketsForANodePool(any(), any())).thenReturn(totalResourceUsage);

    ArgumentCaptor<RecommendClusterRequest> captor = ArgumentCaptor.forClass(RecommendClusterRequest.class);

    assertThat(tasklet.execute(null, chunkContext)).isNull();

    // effectively 1 times
    verify(banzaiRecommenderClient, times(2)).getRecommendation(any(), any(), any(), captor.capture());

    verify(k8sRecommendationDAO, times(1)).insertNodeRecommendationResponse(any(), any(), any(), any(), any());

    return captor.getAllValues().get(1);
  }

  @SneakyThrows
  private RecommendationResponse getRecommendationResponse() {
    // any better way to read file ?
    final URL path = this.getClass().getResource("/recommendation/nodeRecommendationResponse.json");
    final String body = IOUtils.toString(path, StandardCharsets.UTF_8);

    return GSON.fromJson(body, RecommendationResponse.class);
  }
}
