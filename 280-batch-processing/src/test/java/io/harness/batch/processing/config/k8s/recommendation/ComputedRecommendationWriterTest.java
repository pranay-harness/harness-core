package io.harness.batch.processing.config.k8s.recommendation;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.batch.processing.tasklet.support.K8sLabelServiceInfoFetcher;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.K8sWorkload;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerCheckpoint;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ComputedRecommendationWriterTest extends CategoryTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String CLUSTER_ID = "CLUSTER_ID";
  public static final String NAMESPACE = "NAMESPACE";
  public static final String WORKLOAD_NAME = "WORKLOAD_NAME";
  public static final String WORKLOAD_TYPE = "WORKLOAD_TYPE";

  public static final Instant JOB_START_DATE = Instant.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(1));

  private ComputedRecommendationWriter computedRecommendationWriter;

  private WorkloadCostService workloadCostService;
  private WorkloadRecommendationDao workloadRecommendationDao;

  private ArgumentCaptor<K8sWorkloadRecommendation> captor;
  private WorkloadRepository workloadRepository;
  private K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher;

  @Before
  public void setUp() throws Exception {
    workloadCostService = mock(WorkloadCostService.class);
    workloadRecommendationDao = mock(WorkloadRecommendationDao.class);
    workloadRepository = mock(WorkloadRepository.class);
    k8sLabelServiceInfoFetcher = mock(K8sLabelServiceInfoFetcher.class);
    when(workloadRepository.getWorkload(any())).thenReturn(Optional.empty());
    when(k8sLabelServiceInfoFetcher.fetchHarnessServiceInfoFromCache(anyString(), anyMap()))
        .thenReturn(Optional.empty());
    computedRecommendationWriter = new ComputedRecommendationWriter(
        workloadRecommendationDao, workloadCostService, workloadRepository, k8sLabelServiceInfoFetcher, JOB_START_DATE);
    captor = ArgumentCaptor.forClass(K8sWorkloadRecommendation.class);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeResourceChangePercent() throws Exception {
    assertThat(
        ComputedRecommendationWriter.resourceChangePercent(
            ImmutableMap.of("ctr1",
                ContainerRecommendation.builder()
                    .current(ResourceRequirement.builder().request("cpu", "20m").request("memory", "100Mi").build())
                    .guaranteed(ResourceRequirement.builder().request("cpu", "30m").request("memory", "10Mi").build())
                    .build(),
                "ctr2",
                ContainerRecommendation.builder()
                    .current(ResourceRequirement.builder().request("cpu", "0.25").request("memory", "100Mi").build())
                    .guaranteed(ResourceRequirement.builder().request("cpu", "0.5").request("memory", "100Mi").build())
                    .build()),
            "cpu"))
        // cpu change is 20m+0.25->30m+0.5 => 270m->530m => 96.3%
        .isEqualByComparingTo(BigDecimal.valueOf(0.963));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeResourceChangePercentWhenOnlySomeContainersHaveRequests() throws Exception {
    assertThat(
        ComputedRecommendationWriter.resourceChangePercent(
            ImmutableMap.of("ctr1",
                ContainerRecommendation.builder()
                    .current(ResourceRequirement.builder().request("cpu", "20m").request("memory", "100Mi").build())
                    .guaranteed(ResourceRequirement.builder().request("cpu", "30m").request("memory", "10Mi").build())
                    .build(),
                "ctr2",
                ContainerRecommendation.builder()
                    .current(ResourceRequirement.builder().request("memory", "100Mi").build())
                    // don't use this recommendation in change percent, as there's no current cpu here.
                    .guaranteed(ResourceRequirement.builder().request("cpu", "0.5").request("memory", "100Mi").build())
                    .build()),
            "cpu"))
        // cpu change is 20m->30m => 50%
        .isEqualByComparingTo(BigDecimal.valueOf(0.5));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeResourceChangePercentAsNullWhenNoContainersHaveRequests() throws Exception {
    assertThat(
        ComputedRecommendationWriter.resourceChangePercent(
            ImmutableMap.of("ctr1",
                ContainerRecommendation.builder()
                    .current(ResourceRequirement.builder().request("memory", "100Mi").build())
                    .guaranteed(ResourceRequirement.builder().request("cpu", "30m").request("memory", "10Mi").build())
                    .build(),
                "ctr2",
                ContainerRecommendation.builder()
                    .current(ResourceRequirement.builder().request("memory", "100Mi").build())
                    // don't use this recommendation in change percent, as there's no current cpu here.
                    .guaranteed(ResourceRequirement.builder().request("cpu", "0.5").request("memory", "100Mi").build())
                    .build()),
            "cpu"))
        .isNull();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeResourceChangePercentAsZeroWhenNoDifferenceInCurrentAndRecommendation() throws Exception {
    ImmutableMap<String, ContainerRecommendation> containerRecommendations = ImmutableMap.of("ctr1",
        ContainerRecommendation.builder()
            .current(ResourceRequirement.builder().request("cpu", "30m").request("memory", "10Mi").build())
            .guaranteed(ResourceRequirement.builder().request("cpu", "30m").request("memory", "10Mi").build())
            .build());
    assertThat(ComputedRecommendationWriter.resourceChangePercent(containerRecommendations, "cpu")).isZero();
    assertThat(ComputedRecommendationWriter.resourceChangePercent(containerRecommendations, "memory")).isZero();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldEstimateMonthlySavings() throws Exception {
    ImmutableMap<String, ContainerRecommendation> containerRecommendations = ImmutableMap.of("ctr1",
        ContainerRecommendation.builder()
            .current(ResourceRequirement.builder().request("cpu", "20m").request("memory", "100Mi").build())
            .guaranteed(ResourceRequirement.builder().request("cpu", "10m").request("memory", "10Mi").build())
            .build(),
        "ctr2",
        ContainerRecommendation.builder()
            .current(ResourceRequirement.builder().request("cpu", "0.75").request("memory", "100Mi").build())
            .guaranteed(ResourceRequirement.builder().request("cpu", "0.5").request("memory", "75Mi").build())
            .build());

    // cpu change: ((10m+0.5)-(20m+0.75))/(20m+0.75) = (510m-770m)/770m = -0.338
    assertThat(ComputedRecommendationWriter.resourceChangePercent(containerRecommendations, "cpu"))
        .isEqualTo(BigDecimal.valueOf(-0.338));
    // mem change: ((10Mi+75Mi)-(100Mi+100Mi))/(100Mi+100Mi) = (85Mi-200Mi)/200Mi = -0.575
    assertThat(ComputedRecommendationWriter.resourceChangePercent(containerRecommendations, "memory"))
        .isEqualTo(BigDecimal.valueOf(-0.575));

    // last day's cpu & memory total cost
    WorkloadCostService.Cost unitCost =
        WorkloadCostService.Cost.builder().cpu(BigDecimal.valueOf(3.422)).memory(BigDecimal.valueOf(4.234)).build();
    assertThat(computedRecommendationWriter.estimateMonthlySavings(containerRecommendations, unitCost))
        // dailyChange: 3.422*(-0.338) + 4.234*(-0.575) = -3.591
        // monthlySavings = -3.591 * -30 = 107.74
        .isEqualTo(BigDecimal.valueOf(107.74));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testCopyExtendedResources() throws Exception {
    ResourceRequirement current = ResourceRequirement.builder()
                                      .request("cpu", "1")
                                      .request("nvidia.com/gpu", "1")
                                      .limit("nvidia.com/gpu", "2")
                                      .build();
    ResourceRequirement recommended = ResourceRequirement.builder()
                                          .request("cpu", "0.25")
                                          .request("memory", "1G")
                                          .limit("cpu", "1")
                                          .limit("memory", "2G")
                                          .build();
    recommended = ComputedRecommendationWriter.copyExtendedResources(current, recommended);
    assertThat(recommended)
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "0.25")
                       .request("memory", "1G")
                       .limit("cpu", "1")
                       .limit("memory", "2G")
                       .request("nvidia.com/gpu", "1")
                       .limit("nvidia.com/gpu", "2")
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testCopyExtendedResourcesNulls() throws Exception {
    ResourceRequirement current = ResourceRequirement.builder().build();
    ResourceRequirement recommended = ResourceRequirement.builder()
                                          .request("cpu", "0.25")
                                          .request("memory", "1G")
                                          .limit("cpu", "1")
                                          .limit("memory", "2G")
                                          .build();
    recommended = ComputedRecommendationWriter.copyExtendedResources(current, recommended);
    assertThat(recommended)
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "0.25")
                       .request("memory", "1G")
                       .limit("cpu", "1")
                       .limit("memory", "2G")
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldThrowOnNonDirtyWrite() throws Exception {
    List<K8sWorkloadRecommendation> recommendations =
        ImmutableList.of(K8sWorkloadRecommendation.builder().dirty(false).build());
    assertThatThrownBy(() -> computedRecommendationWriter.write(recommendations))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Dirty flag should be set");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeRecommendationsAndSavingsOnWrite() throws Exception {
    List<K8sWorkloadRecommendation> recommendations =
        ImmutableList.of(K8sWorkloadRecommendation.builder()
                             .dirty(true)
                             .accountId(ACCOUNT_ID)
                             .clusterId(CLUSTER_ID)
                             .workloadType(WORKLOAD_TYPE)
                             .namespace(NAMESPACE)
                             .workloadName(WORKLOAD_NAME)
                             .containerRecommendation("harness-example",
                                 ContainerRecommendation.builder()
                                     .current(ResourceRequirement.builder()
                                                  .request("cpu", "1")
                                                  .limit("cpu", "1")
                                                  .request("memory", "1536Mi")
                                                  .limit("memory", "1536Mi")
                                                  .request("nvidia.com/gpu", "1")
                                                  .limit("nvidia.com/gpu", "2")
                                                  .build())
                                     .build())
                             .containerCheckpoint("harness-example",
                                 ContainerCheckpoint.builder()
                                     .lastUpdateTime(Instant.parse("2020-07-28T01:27:20.271Z"))
                                     .cpuHistogram(HistogramCheckpoint.builder()
                                                       .referenceTimestamp(Instant.parse("2020-07-28T00:00:00.000Z"))
                                                       .bucketWeights(ImmutableMap.<Integer, Integer>builder()
                                                                          .put(0, 10000)
                                                                          .put(1, 560)
                                                                          .put(2, 412)
                                                                          .put(3, 340)
                                                                          .put(4, 84)
                                                                          .put(5, 1)
                                                                          .put(36, 1)
                                                                          .build())
                                                       .totalWeight(10.1902752967582)
                                                       .build())
                                     .memoryHistogram(HistogramCheckpoint.builder()
                                                          .referenceTimestamp(Instant.parse("2020-07-21T00:00:00.000Z"))
                                                          .bucketWeights(ImmutableMap.of(23, 3710, 24, 10000))
                                                          .totalWeight(302.138052671595)
                                                          .build())
                                     .firstSampleStart(Instant.parse("2020-07-20T05:52:23.000Z"))
                                     .lastSampleStart(Instant.parse("2020-07-27T13:49:40.000Z"))
                                     .totalSamplesCount(674)
                                     .memoryPeak(460259328L)
                                     .windowEnd(Instant.parse("2020-07-28T05:52:23.000Z"))
                                     .version(1)
                                     .build())
                             .build());
    when(workloadCostService.getLastAvailableDayCost(eq(ResourceId.builder()
                                                             .accountId(ACCOUNT_ID)
                                                             .clusterId(CLUSTER_ID)
                                                             .namespace(NAMESPACE)
                                                             .kind(WORKLOAD_TYPE)
                                                             .name(WORKLOAD_NAME)
                                                             .build()),
             eq(JOB_START_DATE.minus(Duration.ofDays(7)))))
        .thenReturn(WorkloadCostService.Cost.builder()
                        .cpu(BigDecimal.valueOf(3.422))
                        .memory(BigDecimal.valueOf(4.234))
                        .build());

    computedRecommendationWriter.write(recommendations);
    verify(workloadRecommendationDao).save(captor.capture());

    assertThat(captor.getAllValues()).hasSize(1);
    K8sWorkloadRecommendation recommendation = captor.getValue();

    assertThat(recommendation.isDirty()).isFalse();
    assertThat(recommendation.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(recommendation.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(recommendation.getWorkloadType()).isEqualTo(WORKLOAD_TYPE);
    assertThat(recommendation.getNamespace()).isEqualTo(NAMESPACE);
    assertThat(recommendation.getWorkloadName()).isEqualTo(WORKLOAD_NAME);

    Map<String, ContainerRecommendation> containerRecommendations = recommendation.getContainerRecommendations();
    assertThat(containerRecommendations).hasSize(1);

    ContainerRecommendation containerRecommendation = containerRecommendations.get("harness-example");
    assertThat(containerRecommendation.getCurrent())
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "1")
                       .limit("cpu", "1")
                       .request("memory", "1536Mi")
                       .limit("memory", "1536Mi")
                       .request("nvidia.com/gpu", "1")
                       .limit("nvidia.com/gpu", "2")
                       .build());
    assertThat(containerRecommendation.getBurstable())
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "25m")
                       .limit("cpu", "109m")
                       .request("memory", "547M")
                       .limit("memory", "1722M")
                       .request("nvidia.com/gpu", "1")
                       .limit("nvidia.com/gpu", "2")
                       .build());
    assertThat(containerRecommendation.getGuaranteed())
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "25m")
                       .limit("cpu", "25m")
                       .request("memory", "549M")
                       .limit("memory", "549M")
                       .request("nvidia.com/gpu", "1")
                       .limit("nvidia.com/gpu", "2")
                       .build());
    assertThat(containerRecommendation.getNumDays()).isEqualTo(7);
    assertThat(containerRecommendation.getTotalSamplesCount()).isEqualTo(674);

    assertThat(recommendation.getEstimatedSavings()).isEqualByComparingTo(BigDecimal.valueOf(183.80));
    assertThat(recommendation.isLastDayCostAvailable()).isTrue();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldSetLastDayCostAvailableToFalseIfNoWorkloadCost() {
    List<K8sWorkloadRecommendation> recommendations =
        ImmutableList.of(K8sWorkloadRecommendation.builder()
                             .dirty(true)
                             .accountId(ACCOUNT_ID)
                             .clusterId(CLUSTER_ID)
                             .workloadType(WORKLOAD_TYPE)
                             .namespace(NAMESPACE)
                             .workloadName(WORKLOAD_NAME)
                             .containerRecommendation("harness-example",
                                 ContainerRecommendation.builder()
                                     .current(ResourceRequirement.builder()
                                                  .request("cpu", "1")
                                                  .limit("cpu", "1")
                                                  .request("memory", "1536Mi")
                                                  .limit("memory", "1536Mi")
                                                  .request("nvidia.com/gpu", "1")
                                                  .limit("nvidia.com/gpu", "2")
                                                  .build())
                                     .build())
                             .containerCheckpoint("harness-example",
                                 ContainerCheckpoint.builder()
                                     .lastUpdateTime(Instant.parse("2020-07-28T01:27:20.271Z"))
                                     .cpuHistogram(HistogramCheckpoint.builder()
                                                       .referenceTimestamp(Instant.parse("2020-07-28T00:00:00.000Z"))
                                                       .bucketWeights(ImmutableMap.<Integer, Integer>builder()
                                                                          .put(0, 10000)
                                                                          .put(1, 560)
                                                                          .put(2, 412)
                                                                          .put(3, 340)
                                                                          .put(4, 84)
                                                                          .put(5, 1)
                                                                          .put(36, 1)
                                                                          .build())
                                                       .totalWeight(10.1902752967582)
                                                       .build())
                                     .memoryHistogram(HistogramCheckpoint.builder()
                                                          .referenceTimestamp(Instant.parse("2020-07-21T00:00:00.000Z"))
                                                          .bucketWeights(ImmutableMap.of(23, 3710, 24, 10000))
                                                          .totalWeight(302.138052671595)
                                                          .build())
                                     .firstSampleStart(Instant.parse("2020-07-20T05:52:23.000Z"))
                                     .lastSampleStart(Instant.parse("2020-07-27T13:49:40.000Z"))
                                     .totalSamplesCount(674)
                                     .memoryPeak(460259328L)
                                     .windowEnd(Instant.parse("2020-07-28T05:52:23.000Z"))
                                     .version(1)
                                     .build())
                             .build());
    when(workloadCostService.getLastAvailableDayCost(eq(ResourceId.builder()
                                                             .accountId(ACCOUNT_ID)
                                                             .clusterId(CLUSTER_ID)
                                                             .namespace(NAMESPACE)
                                                             .kind(WORKLOAD_TYPE)
                                                             .name(WORKLOAD_NAME)
                                                             .build()),
             eq(JOB_START_DATE.minus(Duration.ofDays(7)))))
        .thenReturn(null);

    computedRecommendationWriter.write(recommendations);
    verify(workloadRecommendationDao).save(captor.capture());

    assertThat(captor.getAllValues()).hasSize(1);
    K8sWorkloadRecommendation recommendation = captor.getValue();

    assertThat(recommendation.isDirty()).isFalse();
    assertThat(recommendation.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(recommendation.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(recommendation.getWorkloadType()).isEqualTo(WORKLOAD_TYPE);
    assertThat(recommendation.getNamespace()).isEqualTo(NAMESPACE);
    assertThat(recommendation.getWorkloadName()).isEqualTo(WORKLOAD_NAME);

    assertThat(recommendation.isLastDayCostAvailable()).isFalse();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldUseCurrentIfLessThanMinResources() throws Exception {
    List<K8sWorkloadRecommendation> recommendations = ImmutableList.of(
        K8sWorkloadRecommendation.builder()
            .dirty(true)
            .accountId(ACCOUNT_ID)
            .clusterId(CLUSTER_ID)
            .workloadType(WORKLOAD_TYPE)
            .namespace(NAMESPACE)
            .workloadName(WORKLOAD_NAME)
            .containerRecommendation("harness-example",
                ContainerRecommendation.builder()
                    .current(ResourceRequirement.builder()
                                 .request("cpu", "15m")
                                 .limit("cpu", "15m")
                                 .request("memory", "20M")
                                 .limit("memory", "20M")
                                 .build())
                    .build())
            .containerCheckpoint("harness-example",
                ContainerCheckpoint.builder()
                    .lastUpdateTime(Instant.parse("2020-08-13T01:02:36.879Z"))
                    .cpuHistogram(HistogramCheckpoint.builder()
                                      .referenceTimestamp(Instant.parse("2020-08-13T00:00:00.000Z"))
                                      .bucketWeights(ImmutableMap.<Integer, Integer>builder().put(0, 10000).build())
                                      .totalWeight(17.0708629762673)
                                      .build())
                    .memoryHistogram(HistogramCheckpoint.builder()
                                         .referenceTimestamp(Instant.parse("2020-08-06T00:00:00.000Z"))
                                         .bucketWeights(ImmutableMap.of(0, 10000))
                                         .totalWeight(233.867887395115)
                                         .build())
                    .firstSampleStart(Instant.parse("2020-08-05T00:25:38.000Z"))
                    .lastSampleStart(Instant.parse("2020-08-12T19:03:01.000Z"))
                    .totalSamplesCount(453)
                    .memoryPeak(3616768L)
                    .windowEnd(Instant.parse("2020-08-13T00:25:38.000Z"))
                    .version(1)
                    .build())
            .build());

    computedRecommendationWriter.write(recommendations);
    verify(workloadRecommendationDao).save(captor.capture());

    assertThat(captor.getAllValues()).hasSize(1);
    K8sWorkloadRecommendation recommendation = captor.getValue();

    assertThat(recommendation.isDirty()).isFalse();
    assertThat(recommendation.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(recommendation.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(recommendation.getWorkloadType()).isEqualTo(WORKLOAD_TYPE);
    assertThat(recommendation.getNamespace()).isEqualTo(NAMESPACE);
    assertThat(recommendation.getWorkloadName()).isEqualTo(WORKLOAD_NAME);

    Map<String, ContainerRecommendation> containerRecommendations = recommendation.getContainerRecommendations();
    assertThat(containerRecommendations).hasSize(1);

    ContainerRecommendation containerRecommendation = containerRecommendations.get("harness-example");
    assertThat(containerRecommendation.getCurrent())
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "15m")
                       .limit("cpu", "15m")
                       .request("memory", "20M")
                       .limit("memory", "20M")
                       .build());
    assertThat(containerRecommendation.getGuaranteed())
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "15m")
                       .limit("cpu", "15m")
                       .request("memory", "20M")
                       .limit("memory", "20M")
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldUseMinResourcesIfLessThanCurrent() throws Exception {
    List<K8sWorkloadRecommendation> recommendations = ImmutableList.of(
        K8sWorkloadRecommendation.builder()
            .dirty(true)
            .accountId(ACCOUNT_ID)
            .clusterId(CLUSTER_ID)
            .workloadType(WORKLOAD_TYPE)
            .namespace(NAMESPACE)
            .workloadName(WORKLOAD_NAME)
            .containerRecommendation("harness-example",
                ContainerRecommendation.builder()
                    .current(ResourceRequirement.builder()
                                 .request("cpu", "1")
                                 .limit("cpu", "1")
                                 .request("memory", "1G")
                                 .limit("memory", "1G")
                                 .build())
                    .build())
            .containerCheckpoint("harness-example",
                ContainerCheckpoint.builder()
                    .lastUpdateTime(Instant.parse("2020-08-13T01:02:36.879Z"))
                    .cpuHistogram(HistogramCheckpoint.builder()
                                      .referenceTimestamp(Instant.parse("2020-08-13T00:00:00.000Z"))
                                      .bucketWeights(ImmutableMap.<Integer, Integer>builder().put(0, 10000).build())
                                      .totalWeight(17.0708629762673)
                                      .build())
                    .memoryHistogram(HistogramCheckpoint.builder()
                                         .referenceTimestamp(Instant.parse("2020-08-06T00:00:00.000Z"))
                                         .bucketWeights(ImmutableMap.of(0, 10000))
                                         .totalWeight(233.867887395115)
                                         .build())
                    .firstSampleStart(Instant.parse("2020-08-05T00:25:38.000Z"))
                    .lastSampleStart(Instant.parse("2020-08-12T19:03:01.000Z"))
                    .totalSamplesCount(453)
                    .memoryPeak(3616768L)
                    .windowEnd(Instant.parse("2020-08-13T00:25:38.000Z"))
                    .version(1)
                    .build())
            .build());

    computedRecommendationWriter.write(recommendations);
    verify(workloadRecommendationDao).save(captor.capture());

    assertThat(captor.getAllValues()).hasSize(1);
    K8sWorkloadRecommendation recommendation = captor.getValue();

    assertThat(recommendation.isDirty()).isFalse();
    assertThat(recommendation.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(recommendation.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(recommendation.getWorkloadType()).isEqualTo(WORKLOAD_TYPE);
    assertThat(recommendation.getNamespace()).isEqualTo(NAMESPACE);
    assertThat(recommendation.getWorkloadName()).isEqualTo(WORKLOAD_NAME);

    Map<String, ContainerRecommendation> containerRecommendations = recommendation.getContainerRecommendations();
    assertThat(containerRecommendations).hasSize(1);

    ContainerRecommendation containerRecommendation = containerRecommendations.get("harness-example");
    assertThat(containerRecommendation.getCurrent())
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "1")
                       .limit("cpu", "1")
                       .request("memory", "1G")
                       .limit("memory", "1G")
                       .build());
    assertThat(containerRecommendation.getGuaranteed())
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "25m")
                       .limit("cpu", "25m")
                       .request("memory", "250M")
                       .limit("memory", "250M")
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldAttachHarnessServiceInfo() throws Exception {
    K8sWorkloadRecommendation k8sWorkloadRecommendation = K8sWorkloadRecommendation.builder().build();
    ResourceId workloadId = ResourceId.builder().accountId("account_id").build();
    when(workloadRepository.getWorkload(workloadId))
        .thenReturn(Optional.of(K8sWorkload.builder().labels(ImmutableMap.of("k1", "v1", "k2", "v2")).build()));
    when(k8sLabelServiceInfoFetcher.fetchHarnessServiceInfoFromCache(
             eq("account_id"), eq(ImmutableMap.of("k1", "v1", "k2", "v2"))))
        .thenReturn(Optional.of(HarnessServiceInfo.builder()
                                    .serviceId("app_id")
                                    .appId("app_id")
                                    .cloudProviderId("cloud_provider_id")
                                    .envId("env_id")
                                    .infraMappingId("infra_mapping_id")
                                    .deploymentSummaryId("deployment_summary_id")
                                    .build()));
    computedRecommendationWriter.addHarnessSvcInfo(workloadId, k8sWorkloadRecommendation);
    assertThat(k8sWorkloadRecommendation.getHarnessServiceInfo())
        .isEqualTo(HarnessServiceInfo.builder()
                       .serviceId("app_id")
                       .appId("app_id")
                       .cloudProviderId("cloud_provider_id")
                       .envId("env_id")
                       .infraMappingId("infra_mapping_id")
                       .deploymentSummaryId("deployment_summary_id")
                       .build());
  }
}
