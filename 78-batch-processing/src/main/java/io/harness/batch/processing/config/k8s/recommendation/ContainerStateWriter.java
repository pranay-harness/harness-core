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
import static java.time.Duration.between;
import static java.util.Collections.emptyMap;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.k8s.WatchEventCostEstimator;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.ContainerStateProto;
import io.harness.grpc.utils.HTimestamps;
import io.harness.histogram.Histogram;
import io.kubernetes.client.custom.Quantity;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerCheckpoint;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
class ContainerStateWriter implements ItemWriter<PublishedMessage> {
  private static final Set<Integer> ACCEPTED_VERSIONS = Collections.singleton(RECOMMENDER_VERSION);

  private final InstanceDataDao instanceDataDao;
  private final WorkloadRecommendationDao workloadRecommendationDao;

  private final Map<ResourceId, WorkloadState> workloadToRecommendation;
  private final LoadingCache<ResourceId, ResourceId> podToWorkload;

  ContainerStateWriter(InstanceDataDao instanceDataDao, WorkloadRecommendationDao workloadRecommendationDao) {
    this.workloadToRecommendation = new HashMap<>();
    this.podToWorkload = Caffeine.newBuilder().maximumSize(10000).build(this ::fetchWorkloadIdForPod);
    this.instanceDataDao = instanceDataDao;
    this.workloadRecommendationDao = workloadRecommendationDao;
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
      ResourceId podId = ResourceId.of(accountId, clusterId, namespace, podName, "Pod");
      ResourceId workloadId = Objects.requireNonNull(podToWorkload.get(podId));
      // intentional reference equality
      if (workloadId == NOT_FOUND) {
        // pod to workload mapping not found in instanceData. Skip this pod.
        return;
      }
      String containerName = containerStateProto.getContainerName();
      WorkloadState workloadState = workloadToRecommendation.computeIfAbsent(workloadId, this ::getWorkloadState);
      Map<String, ContainerState> containerStateMap = workloadState.getContainerStateMap();

      ContainerState containerState = containerStateMap.get(containerName);
      Instant firstSampleStart = HTimestamps.toInstant(containerStateProto.getFirstSampleStart());
      Instant lastSampleStart = HTimestamps.toInstant(containerStateProto.getLastSampleStart());
      if (containerState != null && containerState.getVersion() >= containerStateProto.getVersion()
          && firstSampleStart.isBefore(containerState.getLastSampleStart())) {
        logger.debug("Skipping sample {} as interval already covered", containerStateProto);
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
    updateRecommendations();
  }

  @NotNull
  private WorkloadState getWorkloadState(ResourceId workloadId) {
    return new WorkloadState(workloadRecommendationDao.fetchRecommendationForWorkload(workloadId));
  }

  private void updateRecommendations() {
    this.workloadToRecommendation.forEach((workloadId, workloadState) -> {
      Map<String, ContainerState> containerStates = workloadState.getContainerStateMap();
      Map<String, ContainerCheckpoint> updatedContainerCheckpoints = containerStates.entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, cse -> cse.getValue().toContainerCheckpoint()));
      K8sWorkloadRecommendation recommendation = workloadRecommendationDao.fetchRecommendationForWorkload(workloadId);
      recommendation.setContainerCheckpoints(updatedContainerCheckpoints);
      BigDecimal savings = BigDecimal.ZERO;
      Map<String, ContainerRecommendation> containerRecommendations =
          Optional.ofNullable(recommendation.getContainerRecommendations()).orElseGet(HashMap::new);
      recommendation.setContainerRecommendations(containerRecommendations);
      for (Map.Entry<String, ContainerRecommendation> entry : containerRecommendations.entrySet()) {
        String containerName = entry.getKey();
        ContainerRecommendation containerRecommendation = entry.getValue();
        ContainerState containerState = containerStates.get(containerName);
        if (containerState != null) {
          ResourceRequirement burstable = burstableRecommender().getEstimatedResourceRequirements(containerState);
          containerRecommendation.setBurstable(burstable);
          ResourceRequirement guaranteed = guaranteedRecommender().getEstimatedResourceRequirements(containerState);
          containerRecommendation.setGuaranteed(guaranteed);
          savings = getEstimatedSavings(containerRecommendation);
          long days = between(containerState.getFirstSampleStart(), containerState.getLastSampleStart()).toDays();
          containerRecommendation.setNumDays((int) days);
          containerRecommendation.setTotalSamplesCount(containerState.getTotalSamplesCount());
        }
      }
      recommendation.setEstimatedSavings(Math.max(0, savings.doubleValue()));
      recommendation.setTtl(Instant.now().plus(Duration.ofDays(10)));
      workloadRecommendationDao.save(recommendation);
    });
    workloadToRecommendation.clear();
  }

  private BigDecimal getEstimatedSavings(ContainerRecommendation containerRecommendation) {
    ResourceRequirement current = containerRecommendation.getCurrent();
    ResourceRequirement guaranteed = containerRecommendation.getGuaranteed();
    if (current == null || guaranteed == null) {
      return BigDecimal.ZERO;
    }
    Map<String, String> currentRequests = Optional.ofNullable(current.getRequests()).orElse(emptyMap());
    Map<String, String> guaranteedRequests = Optional.ofNullable(guaranteed.getRequests()).orElse(emptyMap());
    String currentCpu = currentRequests.get("cpu");
    String currentMemory = currentRequests.get("memory");
    String guaranteedCpu = guaranteedRequests.get("cpu");
    String guaranteedMemory = guaranteedRequests.get("memory");
    if (currentCpu == null || currentMemory == null || guaranteedCpu == null || guaranteedMemory == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal currentCpuCores = Quantity.fromString(currentCpu).getNumber();
    BigDecimal currentMemBytes = Quantity.fromString(currentMemory).getNumber();
    BigDecimal guaranteedCpuCores = Quantity.fromString(guaranteedCpu).getNumber();
    BigDecimal guaranteedMemoryBytes = Quantity.fromString(guaranteedMemory).getNumber();
    BigDecimal cpuCoreSavings = currentCpuCores.subtract(guaranteedCpuCores);
    BigDecimal memoryByteSavings = currentMemBytes.subtract(guaranteedMemoryBytes);
    return WatchEventCostEstimator.resourceToCost(cpuCoreSavings, memoryByteSavings);
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
    return ResourceId.of(pod.getAccountId(), pod.getClusterId(), pod.getNamespace(), workloadName, workloadType);
  }
}
