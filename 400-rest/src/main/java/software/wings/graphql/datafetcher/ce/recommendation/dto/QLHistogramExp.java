package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
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
