package io.harness.ccm.dto.graphql.recommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContainerHistogramDTO {
  String containerName;
  HistogramExp cpuHistogram;
  HistogramExp memoryHistogram;

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