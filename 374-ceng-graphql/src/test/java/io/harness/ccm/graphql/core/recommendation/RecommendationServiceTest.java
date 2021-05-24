package io.harness.ccm.graphql.core.recommendation;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceId;
import io.harness.ccm.commons.dao.recommendation.K8sRecommendationDAO;
import io.harness.ccm.graphql.dto.recommendation.ContainerHistogramDTO.HistogramExp;
import io.harness.ccm.graphql.dto.recommendation.FilterStatsDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationItemDTO;
import io.harness.ccm.graphql.dto.recommendation.ResourceType;
import io.harness.ccm.graphql.dto.recommendation.WorkloadRecommendationDTO;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.rule.Owner;
import io.harness.timescaledb.Tables;
import io.harness.timescaledb.tables.pojos.CeRecommendations;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerCheckpoint;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.Cost;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.data.Offset;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RecommendationServiceTest extends CategoryTest {
  private static final String CLUSTER_ID = "clusterId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String NAME = "name";
  private static final String CLUSTER_NAME = "clusterName";
  private static final Double MONTHLY_COST = 100D;
  private static final Double MONTHLY_SAVING = 40D;
  private static final String ID = "id0";
  private static final String NAMESPACE = "namespace";
  private static final String CONTAINER_NAME = "containerName";
  private static final String WORKLOAD_TYPE = "workloadType";

  private static final Offset<Double> DOUBLE_OFFSET = offset(0.1);
  private static final Instant refDate = Instant.now().truncatedTo(ChronoUnit.DAYS);
  private static final List<PartialRecommendationHistogram> histograms =
      Arrays.asList(PartialRecommendationHistogram.builder()
                        .namespace(NAMESPACE)
                        .workloadName(NAME)
                        .workloadType(WORKLOAD_TYPE)
                        .date(refDate.plus(Duration.ofDays(2)))
                        .containerCheckpoints(ImmutableMap.<String, ContainerCheckpoint>builder()
                                                  .put(CONTAINER_NAME,
                                                      ContainerCheckpoint.builder()
                                                          .cpuHistogram(HistogramCheckpoint.builder()
                                                                            .totalWeight(1)
                                                                            .bucketWeights(ImmutableMap.of(0, 10000))
                                                                            .build())
                                                          .memoryPeak(1500000000L)
                                                          .build())
                                                  .build())
                        .build(),
          PartialRecommendationHistogram.builder()
              .namespace(NAMESPACE)
              .workloadName(NAME)
              .workloadType(WORKLOAD_TYPE)
              .date(refDate.plus(Duration.ofDays(3)))
              .containerCheckpoints(ImmutableMap.<String, ContainerCheckpoint>builder()
                                        .put(CONTAINER_NAME,
                                            ContainerCheckpoint.builder()
                                                .cpuHistogram(HistogramCheckpoint.builder()
                                                                  .totalWeight(1)
                                                                  .bucketWeights(ImmutableMap.of(2, 10000))
                                                                  .build())
                                                .memoryPeak(2000000000L)
                                                .build())
                                        .build())
              .build());

  private static final CeRecommendations ceRecommendation = new CeRecommendations()
                                                                .setId(ID)
                                                                .setResourcetype("WORKLOAD")
                                                                .setClustername(CLUSTER_NAME)
                                                                .setName(NAME)
                                                                .setMonthlycost(MONTHLY_COST)
                                                                .setMonthlysaving(MONTHLY_SAVING);

  private static final Cost cost = Cost.builder().cpu(BigDecimal.valueOf(100)).memory(BigDecimal.valueOf(100)).build();

  private static final Map<? extends String, ? extends ContainerRecommendation> containerRecommendationMap =
      ImmutableMap.of(CONTAINER_NAME,
          ContainerRecommendation.builder()
              .lastDayCost(cost)
              .current(ResourceRequirement.builder().request("cpu", "100m").limit("cpu", "2").build())
              .build());

  @Mock private K8sRecommendationDAO k8sRecommendationDAO;
  @InjectMocks private RecommendationService recommendationService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetStats() {
    final RecommendationOverviewStats expectedStats =
        RecommendationOverviewStats.builder().totalMonthlySaving(MONTHLY_SAVING).totalMonthlyCost(MONTHLY_COST).build();

    when(k8sRecommendationDAO.fetchRecommendationsOverviewStats(eq(ACCOUNT_ID), eq(DSL.noCondition())))
        .thenReturn(expectedStats);

    final RecommendationOverviewStats stats = recommendationService.getStats(ACCOUNT_ID, DSL.noCondition());
    assertThat(stats.getTotalMonthlyCost()).isEqualTo(MONTHLY_COST);
    assertThat(stats.getTotalMonthlySaving()).isEqualTo(MONTHLY_SAVING);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testListAllReturnsNoItem() {
    when(k8sRecommendationDAO.fetchRecommendationsOverview(eq(ACCOUNT_ID), any(), eq(0L), eq(10L)))
        .thenReturn(Collections.emptyList());

    final List<RecommendationItemDTO> allRecommendations = recommendationService.listAll(ACCOUNT_ID, null, 0L, 10L);

    assertThat(allRecommendations).isNotNull();
    assertThat(allRecommendations).isEmpty();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testListAllReturnsOneItem() {
    when(k8sRecommendationDAO.fetchRecommendationsOverview(eq(ACCOUNT_ID), any(), eq(0L), eq(10L)))
        .thenReturn(ImmutableList.of(ceRecommendation));

    final List<RecommendationItemDTO> allRecommendations = recommendationService.listAll(ACCOUNT_ID, null, 0L, 10L);

    assertThat(allRecommendations).isNotNull().hasSize(1);
    assertThat(allRecommendations.get(0).getId()).isEqualTo(ID);
    assertThat(allRecommendations.get(0).getResourceType()).isEqualTo(ResourceType.WORKLOAD);
    assertThat(allRecommendations.get(0).getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(allRecommendations.get(0).getRecommendationDetails()).isNull();
    assertThat(allRecommendations.get(0).getResourceName()).isEqualTo(NAME);
    assertThat(allRecommendations.get(0).getMonthlyCost()).isCloseTo(MONTHLY_COST, DOUBLE_OFFSET);
    assertThat(allRecommendations.get(0).getMonthlySaving()).isCloseTo(MONTHLY_SAVING, DOUBLE_OFFSET);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void getWorkloadRecommendationByIdNotFound() {
    when(k8sRecommendationDAO.fetchK8sWorkloadRecommendationById(eq(ACCOUNT_ID), eq(ID))).thenReturn(Optional.empty());

    verify(k8sRecommendationDAO, times(0)).fetchPartialRecommendationHistograms(any(), any(), any(), any());

    final WorkloadRecommendationDTO workloadRecommendationDTO =
        recommendationService.getWorkloadRecommendationById(ACCOUNT_ID, ID, OffsetDateTime.now(), OffsetDateTime.now());

    assertThat(workloadRecommendationDTO).isNotNull();
    assertThat(workloadRecommendationDTO.getItems()).isNotNull().isEmpty();
    assertThat(workloadRecommendationDTO.getLastDayCost()).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetFilterStats() {
    when(k8sRecommendationDAO.getDistinctStringValues(
             eq(ACCOUNT_ID), any(), eq(Tables.CE_RECOMMENDATIONS.RESOURCETYPE), eq(Tables.CE_RECOMMENDATIONS)))
        .thenReturn(ImmutableList.of("v1", "v2"));

    List<FilterStatsDTO> result = recommendationService.getFilterStats(
        ACCOUNT_ID, null, Collections.singletonList("resourceType"), Tables.CE_RECOMMENDATIONS);

    verify(k8sRecommendationDAO, times(1))
        .getDistinctStringValues(
            eq(ACCOUNT_ID), any(), eq(Tables.CE_RECOMMENDATIONS.RESOURCETYPE), eq(Tables.CE_RECOMMENDATIONS));

    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getKey()).isEqualTo("resourceType");
    assertThat(result.get(0).getValues()).containsExactlyInAnyOrder("v1", "v2");
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void getWorkloadRecommendationByIdItemFound() {
    final K8sWorkloadRecommendation workloadRecommendation = K8sWorkloadRecommendation.builder()
                                                                 .workloadName(NAME)
                                                                 .namespace(NAMESPACE)
                                                                 .clusterId(CLUSTER_ID)
                                                                 .accountId(ACCOUNT_ID)
                                                                 .uuid(ID)
                                                                 .lastDayCost(cost)
                                                                 .containerRecommendations(containerRecommendationMap)
                                                                 .workloadType(WORKLOAD_TYPE)
                                                                 .build();

    final ResourceId resourceId = ResourceId.builder()
                                      .accountId(ACCOUNT_ID)
                                      .clusterId(CLUSTER_ID)
                                      .name(NAME)
                                      .namespace(NAMESPACE)
                                      .kind(WORKLOAD_TYPE)
                                      .build();

    when(k8sRecommendationDAO.fetchK8sWorkloadRecommendationById(eq(ACCOUNT_ID), eq(ID)))
        .thenReturn(Optional.of(workloadRecommendation));
    when(k8sRecommendationDAO.fetchPartialRecommendationHistograms(eq(ACCOUNT_ID), eq(resourceId), any(), any()))
        .thenReturn(histograms);

    final WorkloadRecommendationDTO workloadRecommendationDTO =
        recommendationService.getWorkloadRecommendationById(ACCOUNT_ID, ID, OffsetDateTime.now(), OffsetDateTime.now());

    verify(k8sRecommendationDAO, times(1)).fetchPartialRecommendationHistograms(eq(ACCOUNT_ID), any(), any(), any());

    assertThat(workloadRecommendationDTO).isNotNull();
    assertThat(workloadRecommendationDTO.getLastDayCost()).isEqualTo(cost);
    assertThat(workloadRecommendationDTO.getItems()).hasSize(1);
    assertThat(workloadRecommendationDTO.getItems().get(0).getContainerName()).isEqualTo(CONTAINER_NAME);

    assertThat(workloadRecommendationDTO.getItems().get(0).getContainerRecommendation()).isNotNull();
    assertThat(workloadRecommendationDTO.getItems().get(0).getContainerRecommendation().getLastDayCost()).isNotNull();

    Cost containerCost = workloadRecommendationDTO.getItems().get(0).getContainerRecommendation().getLastDayCost();
    assertThat(containerCost).isNotNull().isEqualTo(cost);

    final HistogramExp cpuHistogram = workloadRecommendationDTO.getItems().get(0).getCpuHistogram();
    assertThat(cpuHistogram.getFirstBucketSize()).isEqualTo(0.01);
    assertThat(cpuHistogram.getMinBucket()).isEqualTo(0);
    assertThat(cpuHistogram.getMaxBucket()).isEqualTo(2);
    assertThat(cpuHistogram.getGrowthRatio()).isEqualTo(0.05);
    assertThat(cpuHistogram.getTotalWeight()).isEqualTo(2.0);
    assertThat(cpuHistogram.getNumBuckets()).isEqualTo(3);
    assertThat(cpuHistogram.getBucketWeights()[0]).isEqualTo(1.0);
    assertThat(cpuHistogram.getBucketWeights()[2]).isEqualTo(1.0);

    final HistogramExp memoryHistogram = workloadRecommendationDTO.getItems().get(0).getMemoryHistogram();
    assertThat(memoryHistogram.getFirstBucketSize()).isEqualTo(1E7);
    assertThat(memoryHistogram.getMinBucket()).isEqualTo(43);
    assertThat(memoryHistogram.getMaxBucket()).isEqualTo(49);
    assertThat(memoryHistogram.getGrowthRatio()).isEqualTo(0.05);
    assertThat(memoryHistogram.getTotalWeight()).isEqualTo(2.0);
    assertThat(memoryHistogram.getNumBuckets()).isEqualTo(7);
    assertThat(memoryHistogram.getBucketWeights()[0]).isEqualTo(1.0);
    assertThat(memoryHistogram.getBucketWeights()[6]).isEqualTo(1.0);
  }
}