package software.wings.graphql.datafetcher.ce.recommendation;

import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.CPU_HISTOGRAM_FIRST_BUCKET_SIZE;
import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.HISTOGRAM_BUCKET_SIZE_GROWTH;
import static io.harness.ccm.recommender.k8sworkload.RecommenderUtils.MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
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
              Histogram memoryHistogram = memoryHistograms.get(containerName);
              HistogramCheckpoint memoryHistogramCp = memoryHistogram.saveToCheckpoint();
              Histogram cpuHistogram = cpuHistograms.get(containerName);
              HistogramCheckpoint cpuHistogramCp = cpuHistogram.saveToCheckpoint();
              int numBucketsMemory = RecommenderUtils.MEMORY_HISTOGRAM_OPTIONS.getNumBuckets();
              int numBucketsCpu = RecommenderUtils.CPU_HISTOGRAM_OPTIONS.getNumBuckets();
              double[] memBucketWeights = bucketWeightsMapToArr(memoryHistogramCp, numBucketsMemory);
              StrippedHistogram memStripped = stripZeroes(memBucketWeights, MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE);
              double[] cpuBucketWeights = bucketWeightsMapToArr(cpuHistogramCp, numBucketsCpu);
              StrippedHistogram cpuStripped = stripZeroes(cpuBucketWeights, CPU_HISTOGRAM_FIRST_BUCKET_SIZE);
              return QLContainerHistogramData.builder()
                  .containerName(containerName)
                  .memoryHistogram(QLHistogramExp.builder()
                                       .numBuckets(memStripped.getNumBuckets())
                                       .firstBucketSize(memStripped.getFirstBucketSize())
                                       .growthRatio(HISTOGRAM_BUCKET_SIZE_GROWTH)
                                       .bucketWeights(memStripped.getBucketWeights())
                                       .precomputed(getPrecomputedPercentiles(memoryHistogram))
                                       .totalWeight(memoryHistogramCp.getTotalWeight())
                                       .build())
                  .cpuHistogram(QLHistogramExp.builder()
                                    .numBuckets(cpuStripped.getNumBuckets())
                                    .firstBucketSize(cpuStripped.getFirstBucketSize())
                                    .growthRatio(HISTOGRAM_BUCKET_SIZE_GROWTH)
                                    .bucketWeights(cpuStripped.getBucketWeights())
                                    .precomputed(getPrecomputedPercentiles(cpuHistogram))
                                    .totalWeight(cpuHistogramCp.getTotalWeight())
                                    .build())
                  .build();
            })
            .collect(Collectors.toList());

    return QLK8SWorkloadHistogramData.builder().containerHistogramDataList(containerHistogramDataList).build();
  }

  private double[] getPrecomputedPercentiles(Histogram histogram) {
    double[] result = new double[101];
    for (int p = 1; p <= 100; p++) {
      result[p] = histogram.getPercentile(p / 100.0);
    }
    return result;
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

  private StrippedHistogram stripZeroes(double[] weights, double firstBucketSize) {
    int minBucket = weights.length - 1;
    int maxBucket = 0;
    for (int i = 0; i < weights.length; i++) {
      if (weights[i] > 0) {
        minBucket = Math.min(minBucket, i);
        maxBucket = Math.max(maxBucket, i);
      }
    }
    if (minBucket <= maxBucket) {
      int newNumbuckets = maxBucket - minBucket + 1;
      double newFbs = firstBucketSize * Math.pow(1 + HISTOGRAM_BUCKET_SIZE_GROWTH, minBucket);
      double[] newWeights = Arrays.copyOfRange(weights, minBucket, maxBucket + 1);
      return StrippedHistogram.builder()
          .bucketWeights(newWeights)
          .numBuckets(newNumbuckets)
          .firstBucketSize(newFbs)
          .build();
    }
    return StrippedHistogram.builder().numBuckets(0).bucketWeights(new double[0]).firstBucketSize(0).build();
  }

  @Value
  @Builder
  private static class StrippedHistogram {
    double[] bucketWeights;
    double firstBucketSize;
    int numBuckets;
  }
}
