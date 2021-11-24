package software.wings.graphql.datafetcher.ce.exportData.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CE)
public class QLCEK8sEntityDetails {
  String name;
  String id;
  String nodePoolName;
  String podCapacity;
  String instanceCategory;
  String machineType;
  String qosClass;
  double totalCost;
  double idleCost;
  double unallocatedCost;
  double systemCost;
  double networkCost;
  double storageTotalCost;
  double storageIdleCost;
  double storageUnallocatedCost;
  double storageUtilizationValue;
  double storageRequest;
  double memoryTotalCost;
  double memoryIdleCost;
  double memoryUnallocatedCost;
  double memoryAllocatable;
  double memoryRequested;
  double cpuTotalCost;
  double cpuIdleCost;
  double cpuUnallocatedCost;
  double cpuAllocatable;
  double cpuRequested;
  long createTime;
  long deleteTime;
}
