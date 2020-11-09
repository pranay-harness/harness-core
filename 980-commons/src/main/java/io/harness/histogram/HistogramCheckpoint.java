package io.harness.histogram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.time.Instant;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class HistogramCheckpoint {
  Instant referenceTimestamp;
  @Singular Map<Integer, Integer> bucketWeights;
  double totalWeight;
}
