package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
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
