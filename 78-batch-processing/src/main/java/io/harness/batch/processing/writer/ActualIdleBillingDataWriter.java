package io.harness.batch.processing.writer;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.ActualIdleCostBatchJobData;
import io.harness.batch.processing.ccm.ActualIdleCostData;
import io.harness.batch.processing.ccm.ActualIdleCostWriterData;
import io.harness.batch.processing.entities.InstanceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class ActualIdleBillingDataWriter extends EventWriter implements ItemWriter<ActualIdleCostBatchJobData> {
  @Autowired private BillingDataServiceImpl billingDataService;

  @Override
  public void write(List<? extends ActualIdleCostBatchJobData> list) throws Exception {
    list.forEach(entry -> {
      List<ActualIdleCostData> podsData = entry.getPodData();
      List<ActualIdleCostData> nodesData = entry.getNodeData();
      Map<String, ActualIdleCostData> parentInstanceIdToPodData = getParentInstanceIdToDataMap(podsData);
      nodesData.forEach(nodeData -> {
        String parentInstanceId = nodeData.getInstanceId();
        if (!parentInstanceIdToPodData.containsKey(parentInstanceId)) {
          InstanceData parentInstanceData =
              instanceDataService.fetchInstanceData(nodeData.getAccountId(), parentInstanceId);
          if (null != parentInstanceData) {
            parentInstanceId = parentInstanceData.getInstanceName();
          }
        }
        if (parentInstanceIdToPodData.containsKey(parentInstanceId)) {
          BigDecimal unallocatedCostForNode = BigDecimal.valueOf(nodeData.getCost()
              - parentInstanceIdToPodData.get(parentInstanceId).getCost() - nodeData.getSystemCost());
          BigDecimal cpuUnallocatedCostForNode = BigDecimal.valueOf(nodeData.getCpuCost()
              - parentInstanceIdToPodData.get(parentInstanceId).getCpuCost() - nodeData.getCpuSystemCost());
          BigDecimal memoryUnallocatedCostForNode = BigDecimal.valueOf(nodeData.getMemoryCost()
              - parentInstanceIdToPodData.get(parentInstanceId).getMemoryCost() - nodeData.getMemorySystemCost());
          if (unallocatedCostForNode.compareTo(BigDecimal.ZERO) == -1
              || cpuUnallocatedCostForNode.compareTo(BigDecimal.ZERO) == -1
              || memoryUnallocatedCostForNode.compareTo(BigDecimal.ZERO) == -1) {
            logger.warn(
                "Unallocated billing amount -ve for node account {} cluster {} instance {} startdate {} total {} cpu {} memory {}",
                nodeData.getAccountId(), nodeData.getClusterId(), nodeData.getInstanceId(), nodeData.getStartTime(),
                unallocatedCostForNode, cpuUnallocatedCostForNode, memoryUnallocatedCostForNode);
            unallocatedCostForNode = BigDecimal.ZERO;
            cpuUnallocatedCostForNode = BigDecimal.ZERO;
            memoryUnallocatedCostForNode = BigDecimal.ZERO;
          }
          BigDecimal actualIdleCost = BigDecimal.valueOf(nodeData.getIdleCost() - unallocatedCostForNode.doubleValue());
          BigDecimal cpuActualIdleCost =
              BigDecimal.valueOf(nodeData.getCpuIdleCost() - cpuUnallocatedCostForNode.doubleValue());
          BigDecimal memoryActualIdleCost =
              BigDecimal.valueOf(nodeData.getMemoryIdleCost() - memoryUnallocatedCostForNode.doubleValue());

          if (actualIdleCost.compareTo(BigDecimal.ZERO) == -1 || cpuActualIdleCost.compareTo(BigDecimal.ZERO) == -1
              || memoryActualIdleCost.compareTo(BigDecimal.ZERO) == -1) {
            logger.warn(
                "Unallocated idle cost -ve for node account {} cluster {} instance {} startdate {} total {} cpu {} memory {}",
                nodeData.getAccountId(), nodeData.getClusterId(), nodeData.getInstanceId(), nodeData.getStartTime(),
                actualIdleCost, cpuActualIdleCost, memoryActualIdleCost);
            actualIdleCost = BigDecimal.ZERO;
            cpuActualIdleCost = BigDecimal.ZERO;
            memoryActualIdleCost = BigDecimal.ZERO;
          }

          billingDataService.update(ActualIdleCostWriterData.builder()
                                        .accountId(nodeData.getAccountId())
                                        .instanceId(nodeData.getInstanceId())
                                        .parentInstanceId(nodeData.getParentInstanceId())
                                        .unallocatedCost(unallocatedCostForNode)
                                        .cpuUnallocatedCost(cpuUnallocatedCostForNode)
                                        .memoryUnallocatedCost(memoryUnallocatedCostForNode)
                                        .actualIdleCost(actualIdleCost)
                                        .cpuActualIdleCost(cpuActualIdleCost)
                                        .memoryActualIdleCost(memoryActualIdleCost)
                                        .startTime(nodeData.getStartTime())
                                        .clusterId(nodeData.getClusterId())
                                        .build());
        }
      });
    });
  }

  private Map<String, ActualIdleCostData> getParentInstanceIdToDataMap(List<ActualIdleCostData> podsData) {
    Map<String, ActualIdleCostData> parentInstanceIdToData = new HashMap<>();
    if (podsData != null) {
      podsData.forEach(entry -> { parentInstanceIdToData.put(entry.getParentInstanceId(), entry); });
    }
    return parentInstanceIdToData;
  }
}
