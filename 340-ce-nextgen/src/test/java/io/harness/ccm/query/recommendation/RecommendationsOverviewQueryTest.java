package io.harness.ccm.query.recommendation;

import static io.harness.rule.OwnerRule.UTSAV;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.CPU_HISTOGRAM_FIRST_BUCKET_SIZE;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.HISTOGRAM_BUCKET_SIZE_GROWTH;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.core.recommendation.RecommendationService;
import io.harness.ccm.dto.graphql.recommendation.ContainerHistogramDTO;
import io.harness.ccm.dto.graphql.recommendation.ContainerHistogramDTO.HistogramExp;
import io.harness.ccm.dto.graphql.recommendation.RecommendationDetailsDTO;
import io.harness.ccm.dto.graphql.recommendation.RecommendationItemDTO;
import io.harness.ccm.dto.graphql.recommendation.RecommendationsDTO;
import io.harness.ccm.dto.graphql.recommendation.ResourceType;
import io.harness.ccm.dto.graphql.recommendation.WorkloadRecommendationDTO;
import io.harness.ccm.utils.graphql.GraphQLUtils;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerCheckpoint;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jooq.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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

  private static final Instant refDate = Instant.now().truncatedTo(ChronoUnit.DAYS);

  private static final List<PartialRecommendationHistogram> histograms =
      Arrays.asList(PartialRecommendationHistogram.builder()
                        .namespace(NAMESPACE)
                        .workloadName(NAME)
                        .workloadType("Deployment")
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
              .workloadType("Deployment")
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

  private static final Map<String, ContainerRecommendation> containerRecommendationMap = ImmutableMap.of(CONTAINER_NAME,
      ContainerRecommendation.builder()
          .current(ResourceRequirement.builder().request("cpu", "100m").limit("cpu", "2").build())
          .build());

  @Mock private GraphQLUtils graphQLUtils;
  @Mock private RecommendationService recommendationService;
  @InjectMocks private RecommendationsOverviewQuery overviewQuery;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(graphQLUtils.getAccountIdentifier(any())).thenReturn(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetRecommendationsOverviewQueryWithZeroItems() {
    when(recommendationService.listAll(
             eq(ACCOUNT_ID), any(Condition.class), eq(GraphQLUtils.DEFAULT_OFFSET), eq(GraphQLUtils.DEFAULT_LIMIT)))
        .thenReturn(Collections.emptyList());

    final RecommendationsDTO recommendationsDTO =
        overviewQuery.recommendations(null, null, null, null, null, null, null, null, null, null);

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
        overviewQuery.recommendations(ID, null, null, null, null, null, null, null, null, null);

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
        overviewQuery.recommendations(null, null, null, null, null, null, null, null, null, null);

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