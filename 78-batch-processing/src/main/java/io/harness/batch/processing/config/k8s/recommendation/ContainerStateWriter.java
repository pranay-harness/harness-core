package io.harness.batch.processing.config.k8s.recommendation;

import static io.harness.batch.processing.config.k8s.recommendation.ResourceId.NOT_FOUND;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ContainerResourceRequirementEstimators.burstableRecommender;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ContainerResourceRequirementEstimators.guaranteedRecommender;
import static io.harness.batch.processing.processor.util.InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData;
import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.MEMORY_AGGREGATION_INTERVAL;
import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.RECOMMENDER_VERSION;
import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.newCpuHistogram;
import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.protoToCheckpoint;
import static io.harness.time.DurationUtils.truncate;
import static java.math.RoundingMode.HALF_UP;
import static java.time.Duration.between;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import com.google.common.collect.ImmutableSet;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.harness.batch.processing.config.k8s.recommendation.WorkloadCostService.Cost;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.ContainerStateProto;
import io.harness.grpc.utils.HTimestamps;
import io.harness.histogram.Histogram;
import io.kubernetes.client.custom.Quantity;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerCheckpoint;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
class ContainerStateWriter implements ItemWriter<PublishedMessage> {
  private static final Set<Integer> ACCEPTED_VERSIONS = Collections.singleton(RECOMMENDER_VERSION);

  // How long to keep recommendation, in case of no updates
  public static final Duration RECOMMENDATION_TTL = Duration.ofDays(30);

  private final InstanceDataDao instanceDataDao;
  private final WorkloadRecommendationDao workloadRecommendationDao;
  private final WorkloadCostService workloadCostService;

  private final Map<ResourceId, WorkloadState> workloadToRecommendation;
  private final LoadingCache<ResourceId, ResourceId> podToWorkload;

  ContainerStateWriter(InstanceDataDao instanceDataDao, WorkloadRecommendationDao workloadRecommendationDao,
      WorkloadCostService workloadCostService) {
    this.workloadToRecommendation = new HashMap<>();
    this.podToWorkload = Caffeine.newBuilder().maximumSize(10000).build(this ::fetchWorkloadIdForPod);
    this.instanceDataDao = instanceDataDao;
    this.workloadRecommendationDao = workloadRecommendationDao;
    this.workloadCostService = workloadCostService;
  }

  @Override
  public void write(List<? extends PublishedMessage> items) throws Exception {
    for (PublishedMessage item : items) {
      String accountId = item.getAccountId();
      ContainerStateProto containerStateProto = (ContainerStateProto) item.getMessage();

      if (!ACCEPTED_VERSIONS.contains(containerStateProto.getVersion())) {
        logger.warn("Skip incompatible version: {}. Accepted: {}", containerStateProto.getVersion(), ACCEPTED_VERSIONS);
        return;
      }
      String clusterId = containerStateProto.getClusterId();
      String namespace = containerStateProto.getNamespace();
      String podName = containerStateProto.getPodName();
      ResourceId podId = ResourceId.builder()
                             .accountId(accountId)
                             .clusterId(clusterId)
                             .namespace(namespace)
                             .name(podName)
                             .kind("Pod")
                             .build();
      ResourceId workloadId = Objects.requireNonNull(podToWorkload.get(podId));
      // intentional reference equality
      if (workloadId == NOT_FOUND) {
        // pod to workload mapping not found in instanceData. Skip this pod.
        return;
      }
      WorkloadState workloadState = workloadToRecommendation.computeIfAbsent(workloadId, this ::getWorkloadState);
      updateContainerStateMap(workloadState.getContainerStateMap(), containerStateProto);
    }
    updateRecommendations();
  }

  private void updateContainerStateMap(
      Map<String, ContainerState> containerStateMap, ContainerStateProto containerStateProto) {
    String containerName = containerStateProto.getContainerName();
    ContainerState containerState = containerStateMap.get(containerName);
    Instant firstSampleStart = HTimestamps.toInstant(containerStateProto.getFirstSampleStart());
    Instant lastSampleStart = HTimestamps.toInstant(containerStateProto.getLastSampleStart());
    if (containerState != null && containerState.getVersion() >= containerStateProto.getVersion()
        && firstSampleStart.isBefore(containerState.getLastSampleStart())) {
      logger.info("Skipping sample {} as interval already covered", containerStateProto);
    } else {
      if (containerState == null || containerState.getVersion() < containerStateProto.getVersion()) {
        // First sample seen for this container, or new version of proto for this container
        // Re-initialize containerState
        containerState = new ContainerState();
        containerState.setFirstSampleStart(firstSampleStart);
        containerStateMap.put(containerName, containerState);
        containerState.setVersion(containerStateProto.getVersion());
      }
      containerState.setLastUpdateTime(Instant.now());
      containerState.setLastSampleStart(lastSampleStart);
      containerState.setTotalSamplesCount(
          containerState.getTotalSamplesCount() + containerStateProto.getTotalSamplesCount());

      // Handle cpu
      // Just merge the histogram received from delegate into the existing histogram
      Histogram protoHistogram = newCpuHistogram();
      protoHistogram.loadFromCheckPoint(protoToCheckpoint(containerStateProto.getCpuHistogram()));
      containerState.getCpuHistogram().merge(protoHistogram);

      // Handle memory
      // Treat the memoryPeak received from delegate as a single memory sample to be added to existing histogram
      Instant memoryTs = HTimestamps.toInstant(containerStateProto.getMemoryPeakTime());
      if (containerState.getWindowEnd() == null) {
        containerState.setWindowEnd(memoryTs);
      }

      boolean addNewPeak = false;
      if (memoryTs.isBefore(containerState.getWindowEnd())) {
        long oldMaxMem = containerState.getMemoryPeak();
        if (oldMaxMem != 0 && containerStateProto.getMemoryPeak() > oldMaxMem) {
          containerState.getMemoryHistogram().subtractSample(oldMaxMem, 1.0, containerState.getWindowEnd());
          addNewPeak = true;
        }
      } else {
        // Shift the memory aggregation window to the next interval.
        Duration shift = truncate(between(containerState.getWindowEnd(), memoryTs), MEMORY_AGGREGATION_INTERVAL)
                             .plus(MEMORY_AGGREGATION_INTERVAL);
        containerState.setWindowEnd(containerState.getWindowEnd().plus(shift));
        containerState.setMemoryPeak(0);
        addNewPeak = true;
      }
      if (addNewPeak) {
        containerState.getMemoryHistogram().addSample(
            containerStateProto.getMemoryPeak(), 1.0, containerState.getWindowEnd());
        containerState.setMemoryPeak(containerStateProto.getMemoryPeak());
      }
    }
  }

  @NotNull
  private WorkloadState getWorkloadState(ResourceId workloadId) {
    return new WorkloadState(workloadRecommendationDao.fetchRecommendationForWorkload(workloadId));
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

  /**
   *  Update the cached recommendations into DB.
   */
  private void updateRecommendations() {
    this.workloadToRecommendation.forEach((workloadId, workloadState) -> {
      Map<String, ContainerState> containerStates = workloadState.getContainerStateMap();
      Map<String, ContainerCheckpoint> updatedContainerCheckpoints = containerStates.entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, cse -> cse.getValue().toContainerCheckpoint()));
      K8sWorkloadRecommendation recommendation = workloadRecommendationDao.fetchRecommendationForWorkload(workloadId);
      recommendation.setContainerCheckpoints(updatedContainerCheckpoints);
      Map<String, ContainerRecommendation> containerRecommendations =
          ofNullable(recommendation.getContainerRecommendations()).orElseGet(HashMap::new);
      recommendation.setContainerRecommendations(containerRecommendations);

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
          long days = between(containerState.getFirstSampleStart(), containerState.getLastSampleStart()).toDays();
          containerRecommendation.setNumDays((int) days);
          containerRecommendation.setTotalSamplesCount(containerState.getTotalSamplesCount());
          // indicates we've populated at least one container for a recommendation
          recommendation.setPopulated(true);
        }
      }
      BigDecimal monthlySavings = estimateMonthlySavings(workloadId, containerRecommendations);
      recommendation.setEstimatedSavings(monthlySavings);
      recommendation.setTtl(Instant.now().plus(RECOMMENDATION_TTL));
      workloadRecommendationDao.save(recommendation);
    });
    workloadToRecommendation.clear();
  }

  @Nullable
  BigDecimal estimateMonthlySavings(
      ResourceId workloadId, Map<String, ContainerRecommendation> containerRecommendations) {
    /*
     we have last day's cost for the workload for cpu & memory.
     find percentage diff at workload level, and multiply by the last day's cost to get dailyDiff
     multiply by -30 to convert dailyDiff to  monthly savings.
    */
    BigDecimal cpuChangePercent = resourceChangePercent(containerRecommendations, "cpu");
    BigDecimal memoryChangePercent = resourceChangePercent(containerRecommendations, "memory");
    BigDecimal monthlySavings = null;
    if (cpuChangePercent != null || memoryChangePercent != null) {
      Instant dayEnd = Instant.now().truncatedTo(ChronoUnit.DAYS);
      Instant dayBegin = dayEnd.minus(Duration.ofDays(1));
      Cost lastDayCost = workloadCostService.getActualCost(workloadId, dayBegin, dayEnd);
      if (lastDayCost == null) {
        logger.debug("Not computing savings for {} as lastDayCost is missing", workloadId);
      } else {
        BigDecimal costChangeForDay = BigDecimal.ZERO;
        if (cpuChangePercent != null && lastDayCost.getCpu() != null) {
          costChangeForDay = costChangeForDay.add(cpuChangePercent.multiply(lastDayCost.getCpu()));
        }
        if (memoryChangePercent != null && lastDayCost.getMemory() != null) {
          costChangeForDay = costChangeForDay.add(memoryChangePercent.multiply(lastDayCost.getMemory()));
        }
        monthlySavings = costChangeForDay.multiply(BigDecimal.valueOf(-30)).setScale(2, HALF_UP);
      }
    }
    return monthlySavings;
  }

  /**
   *  Get the percentage change in a resource between current and recommended for the pod, null if un-computable.
   */
  BigDecimal resourceChangePercent(Map<String, ContainerRecommendation> containerRecommendations, String resource) {
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

  @NonNull
  ResourceId fetchWorkloadIdForPod(ResourceId pod) {
    InstanceData podInstance =
        instanceDataDao.getK8sPodInstance(pod.getAccountId(), pod.getClusterId(), pod.getNamespace(), pod.getName());
    if (podInstance == null) {
      logger.warn("Could not find pod {}/{} in instanceData for clusterId={}", pod.getNamespace(), pod.getName(),
          pod.getClusterId());
      return NOT_FOUND;
    }
    String workloadName = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.WORKLOAD_NAME, podInstance);
    String workloadType = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.WORKLOAD_TYPE, podInstance);
    return ResourceId.builder()
        .accountId(pod.getAccountId())
        .clusterId(pod.getClusterId())
        .namespace(pod.getNamespace())
        .name(workloadName)
        .kind(workloadType)
        .build();
  }
}
