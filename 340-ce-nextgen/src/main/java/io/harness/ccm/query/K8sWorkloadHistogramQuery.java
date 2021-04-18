package io.harness.ccm.query;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.CPU_HISTOGRAM_FIRST_BUCKET_SIZE;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.HISTOGRAM_BUCKET_SIZE_GROWTH;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.dto.ContainerHistogramDTO;
import io.harness.ccm.utils.GraphQLUtils;
import io.harness.histogram.Histogram;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialHistogramAggragator;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram.PartialRecommendationHistogramKeys;
import software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
@Singleton
@OwnedBy(CE)
public class K8sWorkloadHistogramQuery {
  @Inject private HPersistence hPersistence;
  @Inject private GraphQLUtils graphQLUtils;

  @GraphQLQuery(name = "workloadHistogram")
  public List<ContainerHistogramDTO> getWorkloadHistogram(@Nullable String clusterId, @Nullable String namespace,
      @Nullable String workloadName, @Nullable String workloadType, @GraphQLNonNull Date startDate,
      @GraphQLNonNull Date endDate, @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountIdentifier = graphQLUtils.getAccountIdentifier(env);

    Query<PartialRecommendationHistogram> query =
        hPersistence.createQuery(PartialRecommendationHistogram.class)
            .filter(PartialRecommendationHistogramKeys.accountId, accountIdentifier);

    if (clusterId != null) {
      query.filter(PartialRecommendationHistogramKeys.clusterId, clusterId);
    }
    if (namespace != null) {
      query.filter(PartialRecommendationHistogramKeys.namespace, namespace);
    }
    if (workloadName != null) {
      query.filter(PartialRecommendationHistogramKeys.workloadName, workloadName);
    }
    if (workloadType != null) {
      query.filter(PartialRecommendationHistogramKeys.workloadType, workloadType);
    }

    query.field(PartialRecommendationHistogramKeys.date)
        .greaterThanOrEq(Instant.ofEpochMilli(startDate.getTime()))
        .field(PartialRecommendationHistogramKeys.date)
        .lessThanOrEq(Instant.ofEpochMilli(endDate.getTime()));

    // find all partial histograms that match this query and merge them
    Map<String, Histogram> cpuHistograms = new HashMap<>();
    Map<String, Histogram> memoryHistograms = new HashMap<>();
    try (HIterator<PartialRecommendationHistogram> partialRecommendationHistograms = new HIterator<>(query.fetch())) {
      PartialHistogramAggragator.aggregateInto(partialRecommendationHistograms, cpuHistograms, memoryHistograms);
    }
    Set<String> containerNames = new HashSet<>(cpuHistograms.keySet());
    containerNames.retainAll(memoryHistograms.keySet());

    // Convert to the output format
    return containerNames.stream()
        .map(containerName -> {
          Histogram memoryHistogram = memoryHistograms.get(containerName);
          checkNotNull(memoryHistogram, "memoryHistogram is null");
          HistogramCheckpoint memoryHistogramCp = memoryHistogram.saveToCheckpoint();
          Histogram cpuHistogram = cpuHistograms.get(containerName);
          HistogramCheckpoint cpuHistogramCp = cpuHistogram.saveToCheckpoint();
          int numBucketsMemory = RecommenderUtils.MEMORY_HISTOGRAM_OPTIONS.getNumBuckets();
          int numBucketsCpu = RecommenderUtils.CPU_HISTOGRAM_OPTIONS.getNumBuckets();
          double[] memBucketWeights = bucketWeightsMapToArr(memoryHistogramCp, numBucketsMemory);
          StrippedHistogram memStripped = stripZeroes(memBucketWeights, MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE);
          double[] cpuBucketWeights = bucketWeightsMapToArr(cpuHistogramCp, numBucketsCpu);
          StrippedHistogram cpuStripped = stripZeroes(cpuBucketWeights, CPU_HISTOGRAM_FIRST_BUCKET_SIZE);
          return ContainerHistogramDTO.builder()
              .containerName(containerName)
              .memoryHistogram(ContainerHistogramDTO.HistogramExp.builder()
                                   .numBuckets(memStripped.getNumBuckets())
                                   .minBucket(memStripped.getMinBucket())
                                   .maxBucket(memStripped.getMaxBucket())
                                   .firstBucketSize(MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE)
                                   .growthRatio(HISTOGRAM_BUCKET_SIZE_GROWTH)
                                   .bucketWeights(memStripped.getBucketWeights())
                                   .precomputed(getPrecomputedPercentiles(memoryHistogram))
                                   .totalWeight(memoryHistogramCp.getTotalWeight())
                                   .build())
              .cpuHistogram(ContainerHistogramDTO.HistogramExp.builder()
                                .numBuckets(cpuStripped.getNumBuckets())
                                .minBucket(cpuStripped.getMinBucket())
                                .maxBucket(cpuStripped.getMaxBucket())
                                .firstBucketSize(CPU_HISTOGRAM_FIRST_BUCKET_SIZE)
                                .growthRatio(HISTOGRAM_BUCKET_SIZE_GROWTH)
                                .bucketWeights(cpuStripped.getBucketWeights())
                                .precomputed(getPrecomputedPercentiles(cpuHistogram))
                                .totalWeight(cpuHistogramCp.getTotalWeight())
                                .build())
              .build();
        })
        .collect(Collectors.toList());
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
      double[] newWeights = Arrays.copyOfRange(weights, minBucket, maxBucket + 1);
      return StrippedHistogram.builder()
          .bucketWeights(newWeights)
          .numBuckets(maxBucket - minBucket + 1)
          .minBucket(minBucket)
          .maxBucket(maxBucket)
          .build();
    }
    return StrippedHistogram.builder().numBuckets(0).bucketWeights(new double[0]).build();
  }

  @Value
  @Builder
  private static class StrippedHistogram {
    double[] bucketWeights;
    int numBuckets;
    int minBucket;
    int maxBucket;
  }
}
