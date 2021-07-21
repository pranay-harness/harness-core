package io.harness.ccm.commons.dao.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceId;
import io.harness.ccm.commons.entities.ClusterRecord;
import io.harness.ccm.commons.entities.k8s.recommendation.K8sWorkloadRecommendation;
import io.harness.ccm.commons.service.intf.ClusterRecordService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.graphql.datafetcher.ce.recommendation.entity.Cost;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(HarnessTeam.CE)
public class RecommendationCrudServiceImplTest extends CategoryTest {
  @Mock private K8sRecommendationDAO k8sRecommendationDAO;
  @Mock private ClusterRecordService clusterRecordService;
  @InjectMocks private RecommendationCrudServiceImpl recommendationCrudService;

  private static final String CLUSTER_ID = "clusterId";
  private static final String CLUSTER_NAME = "clusterName";
  private static final String UUID = "uuid";
  private static final Instant NOW = Instant.now();
  private static final ResourceId RESOURCE_ID =
      ResourceId.builder().accountId("accountId").clusterId(CLUSTER_ID).build();

  private ArgumentCaptor<Double> monthlyCostCaptor;
  private ArgumentCaptor<Double> monthlySavingCaptor;

  @Before
  public void setUp() throws Exception {
    monthlyCostCaptor = ArgumentCaptor.forClass(Double.class);
    monthlySavingCaptor = ArgumentCaptor.forClass(Double.class);

    when(clusterRecordService.get(CLUSTER_ID)).thenReturn(ClusterRecord.builder().clusterName(CLUSTER_NAME).build());
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUpsertWorkloadRecommendation() throws Exception {
    doNothing()
        .when(k8sRecommendationDAO)
        .upsertCeRecommendation(eq(UUID), eq(RESOURCE_ID), eq(CLUSTER_NAME), monthlyCostCaptor.capture(),
            monthlySavingCaptor.capture(), eq(true), eq(NOW));

    recommendationCrudService.upsertWorkloadRecommendation(UUID, RESOURCE_ID, createValidRecommendation());

    verify(clusterRecordService, times(1)).get(eq(CLUSTER_ID));
    verify(k8sRecommendationDAO, times(1));

    assertThat(monthlyCostCaptor.getValue()).isEqualTo(330D);
    assertThat(monthlySavingCaptor.getValue()).isEqualTo(10D);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUpsertWorkloadRecommendationClusterIdNotFound() throws Exception {
    when(clusterRecordService.get(CLUSTER_ID)).thenReturn(null);
    doNothing()
        .when(k8sRecommendationDAO)
        .upsertCeRecommendation(anyString(), any(), anyString(), any(), any(), anyBoolean(), any());

    recommendationCrudService.upsertWorkloadRecommendation(UUID, RESOURCE_ID, createValidRecommendation());

    verify(k8sRecommendationDAO, times(1))
        .upsertCeRecommendation(anyString(), any(), eq(CLUSTER_ID), any(), any(), anyBoolean(), any());
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUpsertWorkloadRecommendationLastDayCostNotAvailable() throws Exception {
    doNothing()
        .when(k8sRecommendationDAO)
        .upsertCeRecommendation(eq(UUID), eq(RESOURCE_ID), eq(CLUSTER_NAME), monthlyCostCaptor.capture(),
            monthlySavingCaptor.capture(), eq(false), eq(Instant.EPOCH));

    recommendationCrudService.upsertWorkloadRecommendation(
        UUID, RESOURCE_ID, K8sWorkloadRecommendation.builder().lastDayCostAvailable(false).build());

    verify(clusterRecordService, times(1)).get(eq(CLUSTER_ID));
    verify(k8sRecommendationDAO, times(1));

    assertThat(monthlyCostCaptor.getValue()).isNull();
    assertThat(monthlySavingCaptor.getValue()).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testUpsertNodeRecommendation() throws Exception {
    final NodePoolId nodePoolId = NodePoolId.builder().clusterid(CLUSTER_ID).build();
    final RecommendationOverviewStats stats =
        RecommendationOverviewStats.builder().totalMonthlySaving(1D).totalMonthlyCost(2D).build();

    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

    doNothing()
        .when(k8sRecommendationDAO)
        .upsertCeRecommendation(eq(UUID), any(), eq(nodePoolId), stringCaptor.capture(), eq(stats), eq(NOW));

    JobConstants jobConstants = mock(JobConstants.class);
    when(jobConstants.getJobEndTime()).thenReturn(NOW.toEpochMilli());

    recommendationCrudService.upsertNodeRecommendation(UUID, jobConstants, nodePoolId, stats);

    assertThat(stringCaptor.getValue()).isNotNull().isEqualTo(CLUSTER_NAME);
  }

  private static K8sWorkloadRecommendation createValidRecommendation() {
    return K8sWorkloadRecommendation.builder()
        .lastDayCostAvailable(true)
        .lastDayCost(Cost.builder().cpu(BigDecimal.TEN).memory(BigDecimal.ONE).build())
        .validRecommendation(true)
        .estimatedSavings(BigDecimal.TEN)
        .lastReceivedUtilDataAt(NOW)
        .numDays(1)
        .build();
  }
}