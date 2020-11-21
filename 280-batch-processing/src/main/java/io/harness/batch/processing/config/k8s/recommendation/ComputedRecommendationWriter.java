package io.harness.batch.processing.config.k8s.recommendation;

import static io.harness.batch.processing.config.k8s.recommendation.estimators.ContainerResourceRequirementEstimators.burstableRecommender;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ContainerResourceRequirementEstimators.guaranteedRecommender;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ContainerResourceRequirementEstimators.recommendedRecommender;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.CPU;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.MEMORY;

import static java.math.RoundingMode.HALF_UP;
import static java.time.Duration.between;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import io.harness.batch.processing.config.k8s.recommendation.WorkloadCostService.Cost;
import io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.batch.processing.tasklet.support.K8sLabelServiceInfoFetcher;
import io.harness.ccm.cluster.entities.K8sWorkload;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import com.google.common.collect.ImmutableSet;
import io.kubernetes.client.custom.Quantity;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.batch.item.ItemWriter;

@Slf4j
class ComputedRecommendationWriter implements ItemWriter<K8sWorkloadRecommendation> {
  public static final Duration RECOMMENDATION_TTL = Duration.ofDays(30);

  private static final long podMinCpuMilliCores = 25L;
  private static final long podMinMemoryBytes = 250_000_000L;

  private final WorkloadRecommendationDao workloadRecommendationDao;
  private final WorkloadCostService workloadCostService;
  private final WorkloadRepository workloadRepository;
  private final K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher;

  private final Instant jobStartDate;

  ComputedRecommendationWriter(WorkloadRecommendationDao workloadRecommendationDao,
      WorkloadCostService workloadCostService, WorkloadRepository workloadRepository,
      K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher, Instant jobStartDate) {
    this.workloadRecommendationDao = workloadRecommendationDao;
    this.workloadCostService = workloadCostService;
    this.workloadRepository = workloadRepository;
    this.k8sLabelServiceInfoFetcher = k8sLabelServiceInfoFetcher;
    this.jobStartDate = jobStartDate;
  }

  void addHarnessSvcInfo(ResourceId workloadId, K8sWorkloadRecommendation k8sWorkloadRecommendation) {
    Map<String, String> labels = workloadRepository.getWorkload(workloadId)
                                     .map(K8sWorkload::getLabels)
                                     .map(K8sWorkload::decodeDotsInKey)
                                     .orElse(emptyMap());
    k8sLabelServiceInfoFetcher.fetchHarnessServiceInfoFromCache(workloadId.getAccountId(), labels)
        .ifPresent(k8sWorkloadRecommendation::setHarnessServiceInfo);
  }

  @Override
  public void write(List<? extends K8sWorkloadRecommendation> items) {
    for (K8sWorkloadRecommendation recommendation : items) {
      if (!recommendation.isDirty()) {
        log.warn("Skipping as dirty flag is not set");
        continue;
      }
      ResourceId workloadId = ResourceId.builder()
                                  .accountId(recommendation.getAccountId())
                                  .clusterId(recommendation.getClusterId())
                                  .kind(recommendation.getWorkloadType())
                                  .namespace(recommendation.getNamespace())
                                  .name(recommendation.getWorkloadName())
                                  .build();
      addHarnessSvcInfo(workloadId, recommendation);
      Map<String, ContainerRecommendation> containerRecommendations =
          ofNullable(recommendation.getContainerRecommendations()).orElseGet(HashMap::new);
      recommendation.setContainerRecommendations(containerRecommendations);

      Map<String, ContainerState> containerStates = new WorkloadState(recommendation).getContainerStateMap();
      int minNumDays = Integer.MAX_VALUE;
      if (isNotEmpty(containerRecommendations)) {
        final double factor = 1.0 / containerRecommendations.size();
        final long containerMinCpuMilliCores = ResourceAmountUtils.scaleResourceAmount(podMinCpuMilliCores, factor);
        final long containerMinMemoryBytes = ResourceAmountUtils.scaleResourceAmount(podMinMemoryBytes, factor);
        for (Map.Entry<String, ContainerRecommendation> entry : containerRecommendations.entrySet()) {
          String containerName = entry.getKey();
          ContainerRecommendation containerRecommendation = entry.getValue();
          ContainerState containerState = containerStates.get(containerName);
          if (containerState != null) {
            ResourceRequirement current = containerRecommendation.getCurrent();
            long curCpuMilliCores = Optional.ofNullable(current)
                                        .map(ResourceRequirement::getRequests)
                                        .map(s -> s.get(CPU))
                                        .map(Quantity::fromString)
                                        .map(Quantity::getNumber)
                                        .map(BigDecimal::doubleValue)
                                        .map(ResourceAmountUtils::cpuAmountFromCores)
                                        .orElse(containerMinCpuMilliCores);
            long curMemoryBytes = Optional.ofNullable(current)
                                      .map(ResourceRequirement::getRequests)
                                      .map(s -> s.get(MEMORY))
                                      .map(Quantity::fromString)
                                      .map(Quantity::getNumber)
                                      .map(BigDecimal::doubleValue)
                                      .map(ResourceAmountUtils::memoryAmountFromBytes)
                                      .orElse(containerMinMemoryBytes);
            // use cur if it's less than min.
            Map<String, Long> minContainerResources =
                ResourceAmountUtils.makeResourceMap(Math.min(containerMinCpuMilliCores, curCpuMilliCores),
                    Math.min(containerMinMemoryBytes, curMemoryBytes));
            ResourceRequirement burstable =
                burstableRecommender(minContainerResources).getEstimatedResourceRequirements(containerState);
            ResourceRequirement guaranteed =
                guaranteedRecommender(minContainerResources).getEstimatedResourceRequirements(containerState);
            ResourceRequirement recommended =
                recommendedRecommender(minContainerResources).getEstimatedResourceRequirements(containerState);
            if (current != null) {
              burstable = copyExtendedResources(current, burstable);
              guaranteed = copyExtendedResources(current, guaranteed);
              recommended = copyExtendedResources(current, recommended);
            }
            containerRecommendation.setBurstable(burstable);
            containerRecommendation.setGuaranteed(guaranteed);
            containerRecommendation.setRecommended(recommended);
            int days =
                (int) between(containerState.getFirstSampleStart(), containerState.getLastSampleStart()).toDays();
            // upper bound by 8 days
            days = Math.min(days, 8);
            containerRecommendation.setNumDays(days);
            minNumDays = Math.min(minNumDays, days);
            containerRecommendation.setTotalSamplesCount(containerState.getTotalSamplesCount());
          }
        }
      }
      recommendation.setNumDays(minNumDays == Integer.MAX_VALUE ? 0 : minNumDays);
      Instant startInclusive = jobStartDate.minus(Duration.ofDays(7));
      Cost lastDayCost = workloadCostService.getLastAvailableDayCost(workloadId, startInclusive);
      if (lastDayCost != null) {
        BigDecimal monthlySavings = estimateMonthlySavings(containerRecommendations, lastDayCost);
        recommendation.setEstimatedSavings(monthlySavings);
        recommendation.setLastDayCostAvailable(true);
      } else {
        recommendation.setLastDayCostAvailable(false);
        log.info("Unable to get lastDayCost for workload {}", workloadId);
      }
      recommendation.setTtl(Instant.now().plus(RECOMMENDATION_TTL));
      recommendation.setDirty(false);
      workloadRecommendationDao.save(recommendation);
    }
  }

  private static Map<String, String> extendedResourcesMap(Map<String, String> resourceMap) {
    ImmutableSet<String> standardResources = ImmutableSet.of("cpu", "memory");
    return ofNullable(resourceMap)
        .orElse(emptyMap())
        .entrySet()
        .stream()
        .filter(e -> !standardResources.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static ResourceRequirement copyExtendedResources(ResourceRequirement current, ResourceRequirement recommended) {
    HashMap<String, String> mergedRequests = new HashMap<>(extendedResourcesMap(current.getRequests()));
    mergedRequests.putAll(ofNullable(recommended.getRequests()).orElse(emptyMap()));
    HashMap<String, String> mergedLimits = new HashMap<>(extendedResourcesMap(current.getLimits()));
    mergedLimits.putAll(ofNullable(recommended.getLimits()).orElse(emptyMap()));
    return ResourceRequirement.builder().requests(mergedRequests).limits(mergedLimits).build();
  }

  @Nullable
  BigDecimal estimateMonthlySavings(
      Map<String, ContainerRecommendation> containerRecommendations, @NotNull Cost lastDayCost) {
    /*
     we have last day's cost for the workload for cpu & memory.
     find percentage diff at workload level, and multiply by the last day's cost to get dailyDiff
     multiply by -30 to convert dailyDiff to  monthly savings.
    */
    BigDecimal cpuChangePercent = resourceChangePercent(containerRecommendations, "cpu");
    BigDecimal memoryChangePercent = resourceChangePercent(containerRecommendations, "memory");
    BigDecimal monthlySavings = null;
    if (cpuChangePercent != null || memoryChangePercent != null) {
      BigDecimal costChangeForDay = BigDecimal.ZERO;
      if (cpuChangePercent != null && lastDayCost.getCpu() != null) {
        costChangeForDay = costChangeForDay.add(cpuChangePercent.multiply(lastDayCost.getCpu()));
      }
      if (memoryChangePercent != null && lastDayCost.getMemory() != null) {
        costChangeForDay = costChangeForDay.add(memoryChangePercent.multiply(lastDayCost.getMemory()));
      }
      monthlySavings = costChangeForDay.multiply(BigDecimal.valueOf(-30)).setScale(2, HALF_UP);
    }
    return monthlySavings;
  }

  /**
   *  Get the percentage change in a resource between current and recommended for the pod, null if un-computable.
   */
  static BigDecimal resourceChangePercent(
      Map<String, ContainerRecommendation> containerRecommendations, String resource) {
    BigDecimal resourceCurrent = BigDecimal.ZERO;
    BigDecimal resourceChange = BigDecimal.ZERO;
    boolean atLeastOneContainerComputable = false;
    for (ContainerRecommendation containerRecommendation : containerRecommendations.values()) {
      BigDecimal current = ofNullable(containerRecommendation.getCurrent())
                               .map(ResourceRequirement::getRequests)
                               .map(requests -> requests.get(resource))
                               .map(Quantity::fromString)
                               .map(Quantity::getNumber)
                               .orElse(null);
      BigDecimal recommended = ofNullable(containerRecommendation.getGuaranteed())
                                   .map(ResourceRequirement::getRequests)
                                   .map(requests -> requests.get(resource))
                                   .map(Quantity::fromString)
                                   .map(Quantity::getNumber)
                                   .orElse(null);
      if (current != null && recommended != null) {
        resourceChange = resourceChange.add(recommended.subtract(current));
        resourceCurrent = resourceCurrent.add(current);
        atLeastOneContainerComputable = true;
      }
    }
    if (atLeastOneContainerComputable && resourceCurrent.compareTo(BigDecimal.ZERO) != 0) {
      return resourceChange.setScale(3, HALF_UP).divide(resourceCurrent, HALF_UP);
    }
    return null;
  }
}
