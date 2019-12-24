package io.harness.batch.processing.ccm;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnallocatedCostData {
  String clusterId;
  String instanceType;
  double cost;
  long startTime;
  long endTime;
}
