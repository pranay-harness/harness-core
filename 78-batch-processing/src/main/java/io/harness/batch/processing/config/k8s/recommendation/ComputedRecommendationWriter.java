package io.harness.batch.processing.config.k8s.recommendation;

import static com.google.common.base.Preconditions.checkState;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ContainerResourceRequirementEstimators.burstableRecommender;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ContainerResourceRequirementEstimators.guaranteedRecommender;
import static java.math.RoundingMode.HALF_UP;
import static java.time.Duration.between;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import com.google.common.collect.ImmutableSet;

import io.harness.batch.processing.config.k8s.recommendation.WorkloadCostService.Cost;
import io.kubernetes.client.custom.Quantity;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.batch.item.ItemWriter;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
class ComputedRecommendationWriter implements ItemWriter<K8sWorkloadRecommendation> {
  public static final Duration RECOMMENDATION_TTL = Duration.ofDays(30);

  private final WorkloadRecommendationDao workloadRecommendationDao;
  private final WorkloadCostService workloadCostService;
  private final Instant jobStartDate;

  ComputedRecommendationWriter(WorkloadRecommendationDao workloadRecommendationDao,
      WorkloadCostService workloadCostService, Instant jobStartDate) {
    this.workloadRecommendationDao = workloadRecommendationDao;
    this.workloadCostService = workloadCostService;
    this.jobStartDate = jobStartDate;
  }

  @Override
  public void write(List<? extends K8sWorkloadRecommendation> items) {
    for (K8sWorkloadRecommendation recommendation : items) {
      checkState(recommendation.isDirty(), "Dirty flag should be set");
      ResourceId workloadId = ResourceId.builder()
                                  .accountId(recommendation.getAccountId())
                                  .clusterId(recommendation.getClusterId())
                                  .kind(recommendation.getWorkloadType())
                                  .namespace(recommendation.getNamespace())
                                  .name(recommendation.getWorkloadName())
                                  .build();
      Map<String, ContainerRecommendation> containerRecommendations =
          ofNullable(recommendation.getContainerRecommendations()).orElseGet(HashMap::new);
      recommendation.setContainerRecommendations(containerRecommendations);

      Map<String, ContainerState> containerStates = new WorkloadState(recommendation).getContainerStateMap();
      int minNumDays = Integer.MAX_VALUE;
      for (Map.Entry<String, ContainerRecommendation> entry : containerRecommendations.entrySet()) {
        String containerName = entry.getKey();
        ContainerRecommendation containerRecommendation = entry.getValue();
        ContainerState containerState = containerStates.get(containerName);
        if (containerState != null) {
          ResourceRequirement burstable = burstableRecommender().getEstimatedResourceRequirements(containerState);
          ResourceRequirement guaranteed = guaranteedRecommender().getEstimatedResourceRequirements(containerState);
          ResourceRequirement current = containerRecommendation.getCurrent();
          if (current != null) {
            burstable = copyExtendedResources(current, burstable);
            guaranteed = copyExtendedResources(current, guaranteed);
          }
          containerRecommendation.setBurstable(burstable);
          containerRecommendation.setGuaranteed(guaranteed);
          int days = (int) between(containerState.getFirstSampleStart(), containerState.getLastSampleStart()).toDays();
          containerRecommendation.setNumDays(days);
          minNumDays = Math.min(minNumDays, days);
          containerRecommendation.setTotalSamplesCount(containerState.getTotalSamplesCount());
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
        logger.info("Unable to get lastDayCost for workload {}", workloadId);
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
