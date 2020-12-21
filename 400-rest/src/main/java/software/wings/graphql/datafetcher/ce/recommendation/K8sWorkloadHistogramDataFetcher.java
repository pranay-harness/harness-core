package software.wings.graphql.datafetcher.ce.recommendation;

import io.harness.ccm.recommender.k8sworkload.RecommenderUtils;
import io.harness.histogram.Histogram;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLContainerHistogramData;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLHistogramExp;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8SWorkloadHistogramData;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadParameters;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerCheckpoint;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram.PartialRecommendationHistogramKeys;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
public class K8sWorkloadHistogramDataFetcher
    extends AbstractObjectDataFetcher<QLK8SWorkloadHistogramData, QLK8sWorkloadParameters> {
  @Inject private HPersistence hPersistence;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLK8SWorkloadHistogramData fetch(QLK8sWorkloadParameters parameters, String accountId) {
    Query<PartialRecommendationHistogram> query = hPersistence.createQuery(PartialRecommendationHistogram.class)
                                                      .field(PartialRecommendationHistogramKeys.accountId)
                                                      .equal(accountId)
                                                      .field(PartialRecommendationHistogramKeys.clusterId)
                                                      .equal(parameters.getCluster())
                                                      .field(PartialRecommendationHistogramKeys.namespace)
                                                      .equal(parameters.getNamespace())
                                                      .field(PartialRecommendationHistogramKeys.workloadName)
                                                      .equal(parameters.getWorkloadName())
                                                      .field(PartialRecommendationHistogramKeys.workloadType)
                                                      .equal(parameters.getWorkloadType())
                                                      .field(PartialRecommendationHistogramKeys.date)
                                                      .greaterThanOrEq(Instant.ofEpochMilli(parameters.getStartDate()))
                                                      .field(PartialRecommendationHistogramKeys.date)
                                                      .lessThanOrEq(Instant.ofEpochMilli(parameters.getEndDate()));
    // find all partial histograms that match this query and merge them
    Map<String, Histogram> cpuHistograms = new HashMap<>();
    Map<String, Histogram> memoryHistograms = new HashMap<>();
    try (HIterator<PartialRecommendationHistogram> partialRecommendationHistograms = new HIterator<>(query.fetch())) {
      for (PartialRecommendationHistogram partialRecommendationHistogram : partialRecommendationHistograms) {
        Map<String, ContainerCheckpoint> containerCheckpoints =
            partialRecommendationHistogram.getContainerCheckpoints();
        for (Map.Entry<String, ContainerCheckpoint> stringContainerCheckpointEntry : containerCheckpoints.entrySet()) {
          String containerName = stringContainerCheckpointEntry.getKey();
          ContainerCheckpoint containerCheckpoint = stringContainerCheckpointEntry.getValue();

          // merge the day's cpu histogram into the aggregate cpu histogram
          HistogramCheckpoint cpuHistogramPartialCheckpoint = containerCheckpoint.getCpuHistogram();
          if (cpuHistogramPartialCheckpoint.getBucketWeights() != null) {
            Histogram cpuHistogramPartial = RecommenderUtils.loadFromCheckpointV2(cpuHistogramPartialCheckpoint);
            Histogram cpuHistogram = cpuHistograms.getOrDefault(containerName, RecommenderUtils.newCpuHistogramV2());
            cpuHistogram.merge(cpuHistogramPartial);
            cpuHistograms.put(containerName, cpuHistogram);
          }

          // add the day's memory peak into the aggregate memory histogram
          long memoryPeak = containerCheckpoint.getMemoryPeak();
          if (memoryPeak != 0) {
            Histogram memoryHistogram =
                memoryHistograms.getOrDefault(containerName, RecommenderUtils.newMemoryHistogramV2());
            memoryHistogram.addSample(memoryPeak, 1.0, Instant.EPOCH); // timestamp is irrelevant since no decay.
            memoryHistograms.put(containerName, memoryHistogram);
          }
        }
      }
    }
    // Convert to the output format
    List<QLContainerHistogramData> containerHistogramDataList =
        cpuHistograms.keySet()
            .stream()
            .map(containerName -> {
              HistogramCheckpoint memoryHistogram = memoryHistograms.get(containerName).saveToCheckpoint();
              HistogramCheckpoint cpuHistogram = cpuHistograms.get(containerName).saveToCheckpoint();
              int numBucketsMemory = RecommenderUtils.MEMORY_HISTOGRAM_OPTIONS.getNumBuckets();
              int numBucketsCpu = RecommenderUtils.CPU_HISTOGRAM_OPTIONS.getNumBuckets();
              return QLContainerHistogramData.builder()
                  .containerName(containerName)
                  .memoryHistogram(QLHistogramExp.builder()
                                       .numBuckets(numBucketsMemory)
                                       .firstBucketSize(RecommenderUtils.MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE)
                                       .growthRatio(RecommenderUtils.HISTOGRAM_BUCKET_SIZE_GROWTH)
                                       .bucketWeights(bucketWeightsMapToArr(memoryHistogram, numBucketsMemory))
                                       .totalWeight(memoryHistogram.getTotalWeight())
                                       .build())
                  .cpuHistogram(QLHistogramExp.builder()
                                    .numBuckets(numBucketsCpu)
                                    .firstBucketSize(RecommenderUtils.CPU_HISTOGRAM_FIRST_BUCKET_SIZE)
                                    .growthRatio(RecommenderUtils.HISTOGRAM_BUCKET_SIZE_GROWTH)
                                    .bucketWeights(bucketWeightsMapToArr(cpuHistogram, numBucketsCpu))
                                    .totalWeight(cpuHistogram.getTotalWeight())
                                    .build())
                  .build();
            })
            .collect(Collectors.toList());

    return QLK8SWorkloadHistogramData.builder().containerHistogramDataList(containerHistogramDataList).build();
  }

  private double[] bucketWeightsMapToArr(HistogramCheckpoint histogram, int numBuckets) {
    double[] bucketWeightsArr = new double[numBuckets];
    long sum = 0;
    for (Integer weight : histogram.getBucketWeights().values()) {
      sum += weight;
    }
    if (sum != 0) {
      double ratio = histogram.getTotalWeight() / sum;
      for (int i = 0; i < numBuckets; i++) {
        bucketWeightsArr[i] = Optional.ofNullable(histogram.getBucketWeights().get(i)).orElse(0) * ratio;
      }
    }
    return bucketWeightsArr;
  }
}
