package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.InstanceType.CLUSTER_UNALLOCATED;
import static io.harness.batch.processing.writer.constants.K8sCCMConstants.UNALLOCATED;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UnallocatedBillingDataServiceImpl;
import io.harness.batch.processing.ccm.ClusterCostData;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.UnallocatedCostData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class UnallocatedBillingDataWriter extends EventWriter implements ItemWriter<List<UnallocatedCostData>> {
  @Autowired private BillingDataServiceImpl billingDataService;
  @Autowired private UnallocatedBillingDataServiceImpl unallocatedBillingDataService;
  private static final String UNALLOCATED_INSTANCE_ID = "unallocated_instance_id_";

  @Override
  public void write(List<? extends List<UnallocatedCostData>> lists) {
    Map<String, ClusterCostData> unallocatedCostMap = new HashMap<>();
    lists.forEach(list -> {
      list.forEach(dataPoint -> {
        if (unallocatedCostMap.containsKey(dataPoint.getClusterId())) {
          ClusterCostData clusterCostData = unallocatedCostMap.get(dataPoint.getClusterId());
          if (dataPoint.getInstanceType().equals(InstanceType.K8S_POD.name())
              || dataPoint.getInstanceType().equals(InstanceType.ECS_TASK_EC2.name())) {
            clusterCostData = clusterCostData.toBuilder().utilizedCost(dataPoint.getCost()).build();
          } else {
            clusterCostData = clusterCostData.toBuilder().totalCost(dataPoint.getCost()).build();
          }
          unallocatedCostMap.replace(dataPoint.getClusterId(), clusterCostData);
        } else {
          unallocatedCostMap.put(dataPoint.getClusterId(),
              dataPoint.getInstanceType().equals(InstanceType.K8S_POD.name())
                      || dataPoint.getInstanceType().equals(InstanceType.ECS_TASK_EC2.name())
                  ? ClusterCostData.builder()
                        .utilizedCost(dataPoint.getCost())
                        .startTime(dataPoint.getStartTime())
                        .endTime(dataPoint.getEndTime())
                        .build()
                  : ClusterCostData.builder()
                        .totalCost(dataPoint.getCost())
                        .startTime(dataPoint.getStartTime())
                        .endTime(dataPoint.getEndTime())
                        .build());
        }
      });
      unallocatedCostMap.forEach((clusterId, clusterCostData) -> {
        ClusterCostData commonFields = unallocatedBillingDataService.getCommonFields(
            clusterId, clusterCostData.getStartTime(), clusterCostData.getEndTime());
        billingDataService.create(
            InstanceBillingData.builder()
                .accountId(commonFields.getAccountId())
                .settingId(commonFields.getSettingId())
                .clusterId(clusterId)
                .instanceType(CLUSTER_UNALLOCATED.name())
                .billingAccountId(commonFields.getBillingAccountId())
                .startTimestamp(clusterCostData.getStartTime())
                .endTimestamp(clusterCostData.getEndTime())
                .billingAmount(BigDecimal.valueOf(clusterCostData.getTotalCost() - clusterCostData.getUtilizedCost()))
                .idleCost(BigDecimal.ZERO)
                .cpuIdleCost(BigDecimal.ZERO)
                .memoryIdleCost(BigDecimal.ZERO)
                .usageDurationSeconds(clusterCostData.getEndTime() - clusterCostData.getStartTime())
                .instanceId(UNALLOCATED_INSTANCE_ID + clusterId)
                .clusterName(commonFields.getClusterName())
                .cpuUnitSeconds(clusterCostData.getEndTime() - clusterCostData.getStartTime())
                .memoryMbSeconds(clusterCostData.getEndTime() - clusterCostData.getStartTime())
                .namespace(UNALLOCATED)
                .region(commonFields.getRegion())
                .clusterType(commonFields.getClusterType())
                .cloudProvider(commonFields.getCloudProvider())
                .workloadName(UNALLOCATED)
                .workloadType(commonFields.getWorkloadType())
                .maxCpuUtilization(1)
                .maxMemoryUtilization(1)
                .avgCpuUtilization(1)
                .avgMemoryUtilization(1)
                .build());
      });
    });
  }
}
