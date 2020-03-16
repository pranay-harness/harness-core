package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.ClusterType.INVALID;
import static io.harness.batch.processing.ccm.InstanceType.CLUSTER_UNALLOCATED;
import static io.harness.batch.processing.writer.constants.K8sCCMConstants.DEFAULT_DEPLOYMENT_TYPE;
import static io.harness.batch.processing.writer.constants.K8sCCMConstants.UNALLOCATED;

import com.google.common.base.Enums;
import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData.InstanceBillingDataBuilder;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UnallocatedBillingDataServiceImpl;
import io.harness.batch.processing.ccm.ClusterCostData;
import io.harness.batch.processing.ccm.ClusterType;
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
            clusterCostData = clusterCostData.toBuilder()
                                  .accountId(dataPoint.getAccountId())
                                  .utilizedCost(dataPoint.getCost())
                                  .cpuUtilizedCost(dataPoint.getCpuCost())
                                  .memoryUtilizedCost(dataPoint.getMemoryCost())
                                  .build();
          } else {
            clusterCostData = clusterCostData.toBuilder()
                                  .accountId(dataPoint.getAccountId())
                                  .totalCost(dataPoint.getCost())
                                  .cpuTotalCost(dataPoint.getCpuCost())
                                  .memoryTotalCost(dataPoint.getMemoryCost())
                                  .totalSystemCost(dataPoint.getSystemCost())
                                  .memorySystemCost(dataPoint.getMemorySystemCost())
                                  .cpuSystemCost(dataPoint.getCpuSystemCost())
                                  .build();
          }
          unallocatedCostMap.replace(dataPoint.getClusterId(), clusterCostData);
        } else {
          unallocatedCostMap.put(dataPoint.getClusterId(),
              dataPoint.getInstanceType().equals(InstanceType.K8S_POD.name())
                      || dataPoint.getInstanceType().equals(InstanceType.ECS_TASK_EC2.name())
                  ? ClusterCostData.builder()
                        .accountId(dataPoint.getAccountId())
                        .utilizedCost(dataPoint.getCost())
                        .cpuUtilizedCost(dataPoint.getCpuCost())
                        .memoryUtilizedCost(dataPoint.getMemoryCost())
                        .startTime(dataPoint.getStartTime())
                        .endTime(dataPoint.getEndTime())
                        .build()
                  : ClusterCostData.builder()
                        .accountId(dataPoint.getAccountId())
                        .totalCost(dataPoint.getCost())
                        .cpuTotalCost(dataPoint.getCpuCost())
                        .memoryTotalCost(dataPoint.getMemoryCost())
                        .totalSystemCost(dataPoint.getSystemCost())
                        .memorySystemCost(dataPoint.getMemorySystemCost())
                        .cpuSystemCost(dataPoint.getCpuSystemCost())
                        .startTime(dataPoint.getStartTime())
                        .endTime(dataPoint.getEndTime())
                        .build());
        }
      });
      unallocatedCostMap.forEach((clusterId, clusterCostData) -> {
        ClusterCostData commonFields = unallocatedBillingDataService.getCommonFields(
            clusterCostData.getAccountId(), clusterId, clusterCostData.getStartTime(), clusterCostData.getEndTime());
        InstanceBillingDataBuilder instanceBillingDataBuilder =
            InstanceBillingData.builder()
                .accountId(commonFields.getAccountId())
                .settingId(commonFields.getSettingId())
                .clusterId(clusterId)
                .instanceType(CLUSTER_UNALLOCATED.name())
                .billingAccountId(commonFields.getBillingAccountId())
                .startTimestamp(clusterCostData.getStartTime())
                .endTimestamp(clusterCostData.getEndTime())
                .billingAmount(BigDecimal.valueOf(clusterCostData.getTotalCost() - clusterCostData.getUtilizedCost()
                    - clusterCostData.getTotalSystemCost()))
                .cpuBillingAmount(BigDecimal.valueOf(clusterCostData.getCpuTotalCost()
                    - clusterCostData.getCpuUtilizedCost() - clusterCostData.getCpuSystemCost()))
                .memoryBillingAmount(BigDecimal.valueOf(clusterCostData.getMemoryTotalCost()
                    - clusterCostData.getMemoryUtilizedCost() - clusterCostData.getMemorySystemCost()))
                .idleCost(BigDecimal.ZERO)
                .cpuIdleCost(BigDecimal.ZERO)
                .memoryIdleCost(BigDecimal.ZERO)
                .systemCost(BigDecimal.ZERO)
                .cpuSystemCost(BigDecimal.ZERO)
                .memorySystemCost(BigDecimal.ZERO)
                .usageDurationSeconds(clusterCostData.getEndTime() - clusterCostData.getStartTime())
                .instanceId(UNALLOCATED_INSTANCE_ID + clusterId)
                .clusterName(commonFields.getClusterName())
                .cpuUnitSeconds(clusterCostData.getEndTime() - clusterCostData.getStartTime())
                .memoryMbSeconds(clusterCostData.getEndTime() - clusterCostData.getStartTime())
                .region(commonFields.getRegion())
                .clusterType(commonFields.getClusterType())
                .cloudProvider(commonFields.getCloudProvider())
                .maxCpuUtilization(1)
                .maxMemoryUtilization(1)
                .avgCpuUtilization(1)
                .avgMemoryUtilization(1);

        switch (Enums.getIfPresent(ClusterType.class, commonFields.getClusterType()).or(INVALID)) {
          case ECS:
            instanceBillingDataBuilder.launchType(UNALLOCATED).cloudServiceName(UNALLOCATED).taskId(UNALLOCATED);
            break;
          case K8S:
            instanceBillingDataBuilder.namespace(UNALLOCATED)
                .workloadName(UNALLOCATED)
                .workloadType(DEFAULT_DEPLOYMENT_TYPE);
            break;
          default:
            throw new IllegalStateException("Unexpected value: " + commonFields.getCloudProvider());
        }

        billingDataService.create(instanceBillingDataBuilder.build());
      });
    });
  }
}
