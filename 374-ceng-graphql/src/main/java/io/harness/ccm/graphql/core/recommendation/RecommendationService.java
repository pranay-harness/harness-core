package io.harness.ccm.graphql.core.recommendation;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.CPU_HISTOGRAM_FIRST_BUCKET_SIZE;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.HISTOGRAM_BUCKET_SIZE_GROWTH;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE;

import io.harness.ccm.commons.beans.recommendation.RecommendationOverviewStats;
import io.harness.ccm.commons.beans.recommendation.ResourceId;
import io.harness.ccm.commons.beans.recommendation.ResourceType;
import io.harness.ccm.commons.dao.recommendation.K8sRecommendationDAO;
import io.harness.ccm.commons.entities.k8s.recommendation.K8sWorkloadRecommendation;
import io.harness.ccm.commons.entities.k8s.recommendation.PartialRecommendationHistogram;
import io.harness.ccm.graphql.dto.recommendation.ContainerHistogramDTO;
import io.harness.ccm.graphql.dto.recommendation.ContainerHistogramDTO.HistogramExp;
import io.harness.ccm.graphql.dto.recommendation.FilterStatsDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationItemDTO;
import io.harness.ccm.graphql.dto.recommendation.WorkloadRecommendationDTO;
import io.harness.histogram.Histogram;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.queryconverter.SQLConverter;
import io.harness.timescaledb.tables.pojos.CeRecommendations;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialHistogramAggragator;
import software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;

@Singleton
public class RecommendationService {
  @Inject private K8sRecommendationDAO k8sRecommendationDAO;

  @NonNull
  public RecommendationOverviewStats getStats(@NonNull final String accountId, Condition condition) {
    return k8sRecommendationDAO.fetchRecommendationsOverviewStats(accountId, condition);
  }

  @NonNull
  public List<RecommendationItemDTO> listAll(
      @NonNull final String accountId, Condition condition, @NonNull Long offset, @NonNull Long limit) {
    final List<CeRecommendations> ceRecommendationsList =
        k8sRecommendationDAO.fetchRecommendationsOverview(accountId, condition, offset, limit);

    return ceRecommendationsList.stream()
        .map(ceRecommendations
            -> RecommendationItemDTO.builder()
                   .id(ceRecommendations.getId())
                   .resourceName(ceRecommendations.getName())
                   .clusterName(ceRecommendations.getClustername())
                   .resourceType(ResourceType.valueOf(ceRecommendations.getResourcetype()))
                   .monthlyCost(ceRecommendations.getMonthlycost())
                   .monthlySaving(ceRecommendations.getMonthlysaving())
                   .build())
        .collect(Collectors.toList());
  }

  @Nullable
  public WorkloadRecommendationDTO getWorkloadRecommendationById(@NonNull final String accountIdentifier, String id,
      @NonNull OffsetDateTime startTime, @NonNull OffsetDateTime endTime) {
    final Optional<K8sWorkloadRecommendation> workloadRecommendation =
        k8sRecommendationDAO.fetchK8sWorkloadRecommendationById(accountIdentifier, id);

    if (!workloadRecommendation.isPresent()) {
      return WorkloadRecommendationDTO.builder().items(Collections.emptyList()).build();
    }

    final List<PartialRecommendationHistogram> histogramList =
        k8sRecommendationDAO.fetchPartialRecommendationHistograms(accountIdentifier,
            constructResourceId(workloadRecommendation.get()), startTime.toInstant(), endTime.toInstant());
    final List<ContainerHistogramDTO> containerHistogramList =
        mergeHistogram(histogramList, workloadRecommendation.get().getContainerRecommendations());

    return WorkloadRecommendationDTO.builder()
        .containerRecommendations(workloadRecommendation.get().getContainerRecommendations())
        .items(containerHistogramList)
        .lastDayCost(workloadRecommendation.get().getLastDayCost())
        .build();
  }

  public List<FilterStatsDTO> getFilterStats(
      String accountId, Condition preCondition, @NonNull List<String> columns, @NonNull Table<?> table) {
    List<FilterStatsDTO> result = new ArrayList<>();

    for (String column : columns) {
      Field<?> field = SQLConverter.getField(column, table);
      List<String> columnValues = k8sRecommendationDAO.getDistinctStringValues(accountId, preCondition, field, table);

      result.add(FilterStatsDTO.builder().key(column).values(columnValues).build());
    }

    return result;
  }

  private static ResourceId constructResourceId(K8sWorkloadRecommendation workloadRecommendation) {
    return ResourceId.builder()
        .accountId(workloadRecommendation.getAccountId())
        .clusterId(workloadRecommendation.getClusterId())
        .kind(workloadRecommendation.getWorkloadType())
        .name(workloadRecommendation.getWorkloadName())
        .namespace(workloadRecommendation.getNamespace())
        .build();
  }

  @NonNull
  private List<ContainerHistogramDTO> mergeHistogram(final List<PartialRecommendationHistogram> histogramList,
      final Map<String, ContainerRecommendation> containerRecommendationMap) {
    // find all partial histograms that match this query and merge them
    final Map<String, Histogram> cpuHistograms = new HashMap<>();
    final Map<String, Histogram> memoryHistograms = new HashMap<>();
    PartialHistogramAggragator.aggregateInto(histogramList, cpuHistograms, memoryHistograms);

    Set<String> containerNames = new HashSet<>(cpuHistograms.keySet());
    containerNames.retainAll(memoryHistograms.keySet());

    // Convert to the output format
    return containerNames.stream()
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
          return ContainerHistogramDTO.builder()
              .containerName(containerName)
              .memoryHistogram(HistogramExp.builder()
                                   .numBuckets(memStripped.getNumBuckets())
                                   .minBucket(memStripped.getMinBucket())
                                   .maxBucket(memStripped.getMaxBucket())
                                   .firstBucketSize(MEMORY_HISTOGRAM_FIRST_BUCKET_SIZE)
                                   .growthRatio(HISTOGRAM_BUCKET_SIZE_GROWTH)
                                   .bucketWeights(memStripped.getBucketWeights())
                                   .precomputed(getPrecomputedPercentiles(memoryHistogram))
                                   .totalWeight(memoryHistogramCp.getTotalWeight())
                                   .build())
              .cpuHistogram(HistogramExp.builder()
                                .numBuckets(cpuStripped.getNumBuckets())
                                .minBucket(cpuStripped.getMinBucket())
                                .maxBucket(cpuStripped.getMaxBucket())
                                .firstBucketSize(CPU_HISTOGRAM_FIRST_BUCKET_SIZE)
                                .growthRatio(HISTOGRAM_BUCKET_SIZE_GROWTH)
                                .bucketWeights(cpuStripped.getBucketWeights())
                                .precomputed(getPrecomputedPercentiles(cpuHistogram))
                                .totalWeight(cpuHistogramCp.getTotalWeight())
                                .build())
              .containerRecommendation(containerRecommendationMap.get(containerName))
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
