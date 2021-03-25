package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLHistogramExp {
  double firstBucketSize;
  double growthRatio;
  int numBuckets;
  int minBucket;
  int maxBucket;
  double[] bucketWeights;
  double totalWeight;
  double[] precomputed;
}
