package io.harness.batch.processing.ccm;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClusterCostData {
  double totalCost;
  double utilizedCost;
  double cpuTotalCost;
  double cpuUtilizedCost;
  double memoryTotalCost;
  double memoryUtilizedCost;
  long startTime;
  long endTime;
  String accountId;
  String billingAccountId;
  String clusterName;
  String settingId;
  String region;
  String cloudProvider;
  String workloadType;
  String clusterType;
}
