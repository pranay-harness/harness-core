package io.harness.ccm.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
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