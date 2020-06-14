package io.harness.histogram;

import com.google.common.collect.ImmutableMap;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class HistogramCheckpoint {
  Instant referenceTimestamp;
  @Singular ImmutableMap<Integer, Integer> bucketWeights;
  double totalWeight;
}
