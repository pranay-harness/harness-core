package io.harness.batch.processing.ccm;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActualIdleCostWriterData {
  String accountId;
  String instanceId;
  String clusterId;
  String parentInstanceId;
  BigDecimal actualIdleCost;
  BigDecimal cpuActualIdleCost;
  BigDecimal memoryActualIdleCost;
  BigDecimal unallocatedCost;
  BigDecimal cpuUnallocatedCost;
  BigDecimal memoryUnallocatedCost;
  long startTime;
  long endTime;
}
