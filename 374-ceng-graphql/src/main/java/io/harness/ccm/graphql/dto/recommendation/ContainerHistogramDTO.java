package io.harness.ccm.graphql.dto.recommendation;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContainerHistogramDTO {
  String containerName;
  HistogramExp cpuHistogram;
  HistogramExp memoryHistogram;
  ContainerRecommendation containerRecommendation;

  @Value
  @Builder
  public static class HistogramExp {
    double firstBucketSize;
    double growthRatio;
    int numBuckets;
    int minBucket;
    int maxBucket;
    double[] bucketWeights;
    double totalWeight;
    double[] precomputed;
  }
}