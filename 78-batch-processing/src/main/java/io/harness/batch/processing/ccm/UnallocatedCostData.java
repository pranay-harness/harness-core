package io.harness.batch.processing.ccm;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnallocatedCostData {
  String accountId;
  String clusterId;
  String instanceType;
  double cost;
  double cpuCost;
  double memoryCost;
  long startTime;
  long endTime;
}
