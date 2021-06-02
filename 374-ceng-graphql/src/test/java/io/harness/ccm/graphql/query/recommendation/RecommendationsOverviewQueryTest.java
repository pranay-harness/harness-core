package io.harness.ccm.graphql.query.recommendation;

import static io.harness.rule.OwnerRule.UTSAV;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.CPU_HISTOGRAM_FIRST_BUCKET_SIZE;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.HISTOGRAM_BUCKET_SIZE_GROWTH;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceType;
import io.harness.ccm.graphql.core.recommendation.RecommendationService;
import io.harness.ccm.graphql.dto.recommendation.ContainerHistogramDTO;
import io.harness.ccm.graphql.dto.recommendation.ContainerHistogramDTO.HistogramExp;
import io.harness.ccm.graphql.dto.recommendation.FilterStatsDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationDetailsDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationItemDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationsDTO;
import io.harness.ccm.graphql.dto.recommendation.WorkloadRecommendationDTO;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.Cost;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jooq.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RecommendationsOverviewQueryTest extends CategoryTest {
  private static final int NUM_BUCKET = 6;
  private static final int MIN_BUCKET = 7;
  private static final int MAX_BUCKET = 8;
  private static final double TOTAL_WEIGHT = 9;

  private static final String ACCOUNT_ID = "accountId";
  private static final String NAME = "name";
  private static final String CLUSTER_NAME = "clusterName";
  private static final Double MONTHLY_COST = 100D;
  private static final Double MONTHLY_SAVING = 40D;
  private static final String NAMESPACE = "namespace";
  private static final String CONTAINER_NAME = "containerName";
  private static final String ID = "id0";

  private static final Map<String, ContainerRecommendation> containerRecommendationMap = ImmutableMap.of(CONTAINER_NAME,
      ContainerRecommendation.builder()
          .current(ResourceRequirement.builder().request("cpu", "100m").limit("cpu", "2").build())
          .build());

  private static final Cost cost =
      Cost.builder().memory(BigDecimal.valueOf(0.116)).cpu(BigDecimal.valueOf(0.4678)).build();

  private ArgumentCaptor<Condition> conditionCaptor;

  @Mock private GraphQLUtils graphQLUtils;
  @Mock private RecommendationService recommendationService;
  @InjectMocks private RecommendationsOverviewQuery overviewQuery;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(graphQLUtils.getAccountIdentifier(any())).thenReturn(ACCOUNT_ID);

    conditionCaptor = ArgumentCaptor.forClass(Condition.class);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationsOverviewQueryWithZeroItems() {
    when(recommendationService.listAll(
             eq(ACCOUNT_ID), any(Condition.class), eq(GraphQLUtils.DEFAULT_OFFSET), eq(GraphQLUtils.DEFAULT_LIMIT)))
        .thenReturn(Collections.emptyList());

    final RecommendationsDTO recommendationsDTO =
        overviewQuery.recommendations(null, null, null, null, null, null, null, 0L, 10L, null);

    assertRecommendationOverviewListResponse(recommendationsDTO);
    assertThat(recommendationsDTO.getItems()).isEmpty();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationsOverviewQueryWithIdFilter() {
    when(recommendationService.listAll(
             eq(ACCOUNT_ID), any(Condition.class), eq(GraphQLUtils.DEFAULT_OFFSET), eq(GraphQLUtils.DEFAULT_LIMIT)))
        .thenReturn(ImmutableList.of(createRecommendationItem("id0", ResourceType.WORKLOAD)));

    final RecommendationsDTO recommendationsDTO =
        overviewQuery.recommendations(ID, null, null, null, null, null, null, 0L, 10L, null);

    assertRecommendationOverviewListResponse(recommendationsDTO);
    assertThat(recommendationsDTO.getItems()).containsExactly(createRecommendationItem("id0"));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationsOverviewQueryWithNoFilter() {
    when(recommendationService.listAll(eq(ACCOUNT_ID), any(Condition.class), any(), any()))
        .thenReturn(ImmutableList.of(createRecommendationItem("id0"), createRecommendationItem("id1")));

    final RecommendationsDTO recommendationsDTO =
        overviewQuery.recommendations(null, null, null, null, null, null, null, 0L, 10L, null);

    assertRecommendationOverviewListResponse(recommendationsDTO);
    assertThat(recommendationsDTO.getItems())
        .containsExactlyInAnyOrder(createRecommendationItem("id0"), createRecommendationItem("id1"));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationsOverviewQueryWithAllFiltersExceptId() {
    when(recommendationService.listAll(eq(ACCOUNT_ID), any(Condition.class), any(), any()))
        .thenReturn(ImmutableList.of(createRecommendationItem("id0"), createRecommendationItem("id1")));

    final RecommendationsDTO recommendationsDTO = overviewQuery.recommendations(
        null, NAME, NAMESPACE, CLUSTER_NAME, ResourceType.WORKLOAD, 0D, 0D, 0L, 10L, null);

    assertRecommendationOverviewListResponse(recommendationsDTO);
    assertThat(recommendationsDTO.getItems())
        .containsExactlyInAnyOrder(createRecommendationItem("id0"), createRecommendationItem("id1"));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationDetailsByIdInRecommendationItemContext() {
    when(recommendationService.getWorkloadRecommendationById(eq(ACCOUNT_ID), eq(ID), any(), any()))
        .thenReturn(createWorkloadRecommendation());

    final RecommendationDetailsDTO recommendationDetails =
        overviewQuery.recommendationDetails(createRecommendationItem(ID), null, null, null);

    assertWorkloadRecommendationDetails(recommendationDetails);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationDetailsByIdAsParentQuery() {
    when(recommendationService.getWorkloadRecommendationById(eq(ACCOUNT_ID), eq(ID), any(), any()))
        .thenReturn(createWorkloadRecommendation());

    final RecommendationDetailsDTO recommendationDetails =
        overviewQuery.recommendationDetails(ID, ResourceType.WORKLOAD, null, null, null);

    assertWorkloadRecommendationDetails(recommendationDetails);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationDetailsByIdInRecommendationItemContextNotFound() {
    when(recommendationService.getWorkloadRecommendationById(eq(ACCOUNT_ID), eq(ID), any(), any()))
        .thenReturn(WorkloadRecommendationDTO.builder().items(Collections.emptyList()).build());

    final RecommendationDetailsDTO recommendationDetails =
        overviewQuery.recommendationDetails(createRecommendationItem(ID), null, null, null);

    assertWorkloadRecommendationByIdNotFound(recommendationDetails);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationDetailsByIdAsParentQueryNotFound() {
    when(recommendationService.getWorkloadRecommendationById(eq(ACCOUNT_ID), eq(ID), any(), any()))
        .thenReturn(WorkloadRecommendationDTO.builder().items(Collections.emptyList()).build());

    final RecommendationDetailsDTO recommendationDetails =
        overviewQuery.recommendationDetails(ID, ResourceType.WORKLOAD, null, null, null);

    assertWorkloadRecommendationByIdNotFound(recommendationDetails);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testRecommendationFilterStats() {
    String columnName = "resourceType";
    List<String> columns = Collections.singletonList(columnName);
    List<FilterStatsDTO> actualResponse =
        ImmutableList.of(FilterStatsDTO.builder()
                             .key(columnName)
                             .values(ImmutableList.of(ResourceType.WORKLOAD.name(), ResourceType.NODE_POOL.name()))
                             .build());

    when(recommendationService.getFilterStats(eq(ACCOUNT_ID), any(), eq(columns), eq(CE_RECOMMENDATIONS)))
        .thenReturn(actualResponse);

    List<FilterStatsDTO> result =
        overviewQuery.recommendationFilterStats(columns, null, null, null, null, null, null, null);

    verify(recommendationService, times(1)).getFilterStats(any(), conditionCaptor.capture(), any(), any());

    assertThat(result).isNotNull().hasSize(1);
    assertThat(result.get(0).getKey()).isEqualTo(columnName);
    assertThat(result.get(0).getValues())
        .containsExactlyInAnyOrder(ResourceType.WORKLOAD.name(), ResourceType.NODE_POOL.name());

    assertCommonCondition(conditionCaptor.getValue());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testRecommendationFilterStatsWithPreselectedFilters() {
    String columnName = "resourceType";
    List<String> columns = Collections.singletonList(columnName);
    List<FilterStatsDTO> actualResponse = ImmutableList.of(
        FilterStatsDTO.builder().key(columnName).values(ImmutableList.of(ResourceType.WORKLOAD.name())).build());

    when(recommendationService.getFilterStats(eq(ACCOUNT_ID), any(), eq(columns), eq(CE_RECOMMENDATIONS)))
        .thenReturn(actualResponse);

    List<FilterStatsDTO> result = overviewQuery.recommendationFilterStats(
        columns, "name0", "namespace0", "clusterName0", ResourceType.WORKLOAD, 200D, 100D, null);

    verify(recommendationService, times(1)).getFilterStats(any(), conditionCaptor.capture(), any(), any());

    assertThat(result).isNotNull().hasSize(1);
    assertThat(result.get(0).getKey()).isEqualTo(columnName);
    assertThat(result.get(0).getValues()).containsExactly(ResourceType.WORKLOAD.name());

    Condition condition = conditionCaptor.getValue();
    assertCommonCondition(condition);

    assertThat(condition.toString())
        .contains(CE_RECOMMENDATIONS.NAME.getQualifiedName().toString())
        .contains("name0")
        .contains(CE_RECOMMENDATIONS.NAMESPACE.getQualifiedName().toString())
        .contains("namespace0")
        .contains(CE_RECOMMENDATIONS.CLUSTERNAME.getQualifiedName().toString())
        .contains("clusterName0")
        .contains(CE_RECOMMENDATIONS.RESOURCETYPE.getQualifiedName().toString())
        .contains(ResourceType.WORKLOAD.name())
        .contains(CE_RECOMMENDATIONS.MONTHLYCOST.getQualifiedName().toString())
        .contains("100")
        .contains(CE_RECOMMENDATIONS.MONTHLYSAVING.getQualifiedName().toString())
        .contains("200");
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testRecommendationStats() {
    when(recommendationService.getStats(eq(ACCOUNT_ID), any()))
        .thenReturn(RecommendationOverviewStats.builder().totalMonthlyCost(100D).totalMonthlySaving(100D).build());

    RecommendationOverviewStats stats =
        overviewQuery.recommendationStats(null, null, null, null, null, null, null, null);

    verify(recommendationService, times(1)).getStats(any(), conditionCaptor.capture());

    assertThat(stats).isNotNull();
    assertThat(stats.getTotalMonthlyCost()).isCloseTo(100D, offset(0.5D));
    assertThat(stats.getTotalMonthlySaving()).isCloseTo(100D, offset(0.5D));

    assertCommonCondition(conditionCaptor.getValue());
  }

  private void assertCommonCondition(Condition condition) {
    assertThat(condition).isNotNull();
    assertThat(condition.toString())
        .contains(CE_RECOMMENDATIONS.ISVALID.getQualifiedName().toString())
        .contains("true")
        .contains(CE_RECOMMENDATIONS.LASTPROCESSEDAT.getQualifiedName().toString());
  }

  private void assertRecommendationOverviewListResponse(final RecommendationsDTO recommendationsDTO) {
    assertThat(recommendationsDTO).isNotNull();
    assertThat(recommendationsDTO.getLimit()).isEqualTo(GraphQLUtils.DEFAULT_LIMIT);
    assertThat(recommendationsDTO.getOffset()).isEqualTo(GraphQLUtils.DEFAULT_OFFSET);
    assertThat(recommendationsDTO.getItems()).isNotNull();
  }

  private static WorkloadRecommendationDTO createWorkloadRecommendation() {
    return WorkloadRecommendationDTO.builder()
        .items(ImmutableList.of(createContainerHistogram()))
        .containerRecommendations(containerRecommendationMap)
        .lastDayCost(cost)
        .build();
  }

  private static ContainerHistogramDTO createContainerHistogram() {
    return ContainerHistogramDTO.builder()
        .containerName(CONTAINER_NAME)
        .memoryHistogram(HistogramExp.builder()
                             .numBuckets(NUM_BUCKET)
                             .minBucket(MIN_BUCKET)
                             .maxBucket(MAX_BUCKET)
                             .firstBucketSize(MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE)
                             .growthRatio(HISTOGRAM_BUCKET_SIZE_GROWTH)
                             .bucketWeights(new double[] {1, 2})
                             .precomputed(new double[] {2, 3})
                             .totalWeight(TOTAL_WEIGHT)
                             .build())
        .cpuHistogram(HistogramExp.builder()
                          .numBuckets(NUM_BUCKET)
                          .minBucket(MIN_BUCKET)
                          .maxBucket(MAX_BUCKET)
                          .firstBucketSize(CPU_HISTOGRAM_FIRST_BUCKET_SIZE)
                          .growthRatio(HISTOGRAM_BUCKET_SIZE_GROWTH)
                          .bucketWeights(new double[] {3, 4})
                          .precomputed(new double[] {4, 5})
                          .totalWeight(TOTAL_WEIGHT)
                          .build())
        .build();
  }

  private void assertWorkloadRecommendationDetails(final RecommendationDetailsDTO recommendationDetails) {
    assertThat(recommendationDetails).isNotNull();
    assertThat(recommendationDetails).isExactlyInstanceOf(WorkloadRecommendationDTO.class);

    final WorkloadRecommendationDTO workloadRecommendationDTO = (WorkloadRecommendationDTO) recommendationDetails;
    assertThat(workloadRecommendationDTO.getContainerRecommendations())
        .hasSize(1)
        .containsExactlyInAnyOrderEntriesOf(containerRecommendationMap);
    assertThat(workloadRecommendationDTO.getLastDayCost()).isEqualTo(cost);
    assertThat(workloadRecommendationDTO.getItems()).hasSize(1);
    assertThat(workloadRecommendationDTO.getItems().get(0).getContainerName()).isEqualTo(CONTAINER_NAME);

    final HistogramExp cpuHistogram = workloadRecommendationDTO.getItems().get(0).getCpuHistogram();

    assertHistogram(cpuHistogram, CPU_HISTOGRAM_FIRST_BUCKET_SIZE);
    assertThat(cpuHistogram.getBucketWeights()[0]).isEqualTo(3.0);
    assertThat(cpuHistogram.getBucketWeights()[1]).isEqualTo(4.0);

    final HistogramExp memoryHistogram = workloadRecommendationDTO.getItems().get(0).getMemoryHistogram();

    assertHistogram(memoryHistogram, MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE);
    assertThat(memoryHistogram.getBucketWeights()[0]).isEqualTo(1.0);
    assertThat(memoryHistogram.getBucketWeights()[1]).isEqualTo(2.0);
  }

  private void assertHistogram(HistogramExp histogramExp, double firstBucketSize) {
    assertThat(histogramExp.getFirstBucketSize()).isEqualTo(firstBucketSize);
    assertThat(histogramExp.getMinBucket()).isEqualTo(MIN_BUCKET);
    assertThat(histogramExp.getMaxBucket()).isEqualTo(MAX_BUCKET);
    assertThat(histogramExp.getGrowthRatio()).isEqualTo(HISTOGRAM_BUCKET_SIZE_GROWTH);
    assertThat(histogramExp.getTotalWeight()).isEqualTo(TOTAL_WEIGHT);
    assertThat(histogramExp.getNumBuckets()).isEqualTo(NUM_BUCKET);
  }

  private void assertWorkloadRecommendationByIdNotFound(final RecommendationDetailsDTO recommendationDetails) {
    assertThat(recommendationDetails).isNotNull();
    assertThat(recommendationDetails).isExactlyInstanceOf(WorkloadRecommendationDTO.class);
    assertThat(recommendationDetails)
        .isEqualTo(WorkloadRecommendationDTO.builder().items(Collections.emptyList()).build());
  }

  private static RecommendationItemDTO createRecommendationItem(String id) {
    return createRecommendationItem(id, ResourceType.WORKLOAD);
  }

  private static RecommendationItemDTO createRecommendationItem(String id, ResourceType resourceType) {
    return RecommendationItemDTO.builder()
        .id(id)
        .clusterName(CLUSTER_NAME)
        .monthlyCost(MONTHLY_COST)
        .monthlySaving(MONTHLY_SAVING)
        .resourceName(NAME)
        .recommendationDetails(null)
        .resourceType(resourceType)
        .build();
  }
}