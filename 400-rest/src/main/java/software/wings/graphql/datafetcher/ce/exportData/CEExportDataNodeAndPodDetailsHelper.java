/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.exportData;

import software.wings.graphql.datafetcher.billing.NodeAndPodDetailsDataFetcher;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEAggregation;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEData;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEDataEntry;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEEntityGroupBy;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEFilter;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEGroupBy;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEK8sEntity;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEK8sEntityDetails;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCESort;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCESortType;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortType;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLNodeAndPodDetailsTableData;
import software.wings.graphql.schema.type.aggregation.billing.QLNodeAndPodDetailsTableRow;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CEExportDataNodeAndPodDetailsHelper {
  @Inject NodeAndPodDetailsDataFetcher nodeAndPodDetailsDataFetcher;

  public QLCEData getNodeAndPodData(String accountId, List<QLCEAggregation> aggregateFunction, List<QLCEFilter> filters,
      List<QLCEGroupBy> groupBy, List<QLCESort> sort, Integer limit, Integer offset, boolean skipRoundOff) {
    return convertNodeAndPodData(nodeAndPodDetailsDataFetcher.getNodeAndPodData(accountId, convertFilters(filters),
        convertAggregations(aggregateFunction), convertGroupBys(groupBy), convertSort(sort), limit, offset,
        skipRoundOff));
  }

  public boolean isNodeAndPodQuery(List<QLCEEntityGroupBy> groupByList) {
    return groupByList.stream().anyMatch(
        groupBy -> groupBy == QLCEEntityGroupBy.Node || groupBy == QLCEEntityGroupBy.Pod);
  }

  private List<QLCCMAggregationFunction> convertAggregations(List<QLCEAggregation> aggregateFunctions) {
    List<QLCCMAggregationFunction> convertedAggregations = new ArrayList<>();
    aggregateFunctions.forEach(aggregation -> {
      String columnName = getAggregationColumnName(aggregation);
      QLCCMAggregateOperation operation = getAggregationOperation(aggregation);
      convertedAggregations.add(
          QLCCMAggregationFunction.builder().columnName(columnName).operationType(operation).build());
    });
    return convertedAggregations;
  }

  private List<QLBillingDataFilter> convertFilters(List<QLCEFilter> filters) {
    List<QLBillingDataFilter> convertedFilters = new ArrayList<>();
    filters.forEach(filter
        -> convertedFilters.add(QLBillingDataFilter.builder()
                                    .cluster(filter.getCluster())
                                    .instanceType(filter.getInstanceType())
                                    .namespace(filter.getNamespace())
                                    .workloadName(filter.getWorkload())
                                    .nodeInstanceId(filter.getNode())
                                    .podInstanceId(filter.getPod())
                                    .startTime(filter.getStartTime())
                                    .endTime(filter.getEndTime())
                                    .build()));
    return convertedFilters;
  }

  private List<QLCCMGroupBy> convertGroupBys(List<QLCEGroupBy> groupBys) {
    List<QLCCMGroupBy> convertedGroupBys = new ArrayList<>();
    groupBys.forEach(groupBy -> {
      if (groupBy.getEntity() == QLCEEntityGroupBy.Node) {
        convertedGroupBys.add(QLCCMGroupBy.builder().entityGroupBy(QLCCMEntityGroupBy.Node).build());
      } else if (groupBy.getEntity() == QLCEEntityGroupBy.Pod) {
        convertedGroupBys.add(QLCCMGroupBy.builder().entityGroupBy(QLCCMEntityGroupBy.Pod).build());
      } else if (groupBy.getEntity() == QLCEEntityGroupBy.Cluster) {
        convertedGroupBys.add(QLCCMGroupBy.builder().entityGroupBy(QLCCMEntityGroupBy.Cluster).build());
      }
    });
    return convertedGroupBys;
  }

  private List<QLBillingSortCriteria> convertSort(List<QLCESort> sortList) {
    List<QLBillingSortCriteria> convertedSort = new ArrayList<>();
    sortList.forEach(sort -> {
      if (sort.getSortType() == QLCESortType.TOTALCOST) {
        convertedSort.add(
            QLBillingSortCriteria.builder().sortOrder(sort.getOrder()).sortType(QLBillingSortType.Amount).build());
      } else if (sort.getSortType() == QLCESortType.IDLECOST) {
        convertedSort.add(
            QLBillingSortCriteria.builder().sortOrder(sort.getOrder()).sortType(QLBillingSortType.IdleCost).build());
      }
    });
    return convertedSort;
  }

  private String getAggregationColumnName(QLCEAggregation aggregation) {
    if (aggregation.getCost() != null) {
      return aggregation.getCost().toString();
    } else if (aggregation.getUtilization() != null) {
      return aggregation.getUtilization().toString();
    }
    return null;
  }

  private QLCCMAggregateOperation getAggregationOperation(QLCEAggregation aggregation) {
    switch (aggregation.getFunction()) {
      case AVG:
        return QLCCMAggregateOperation.AVG;
      case SUM:
        return QLCCMAggregateOperation.SUM;
      case MAX:
        return QLCCMAggregateOperation.MAX;
      case MIN:
        return QLCCMAggregateOperation.MIN;
      default:
        return null;
    }
  }

  private QLCEData convertNodeAndPodData(QLNodeAndPodDetailsTableData nodeAndPodData) {
    List<QLNodeAndPodDetailsTableRow> data = nodeAndPodData.getData();
    List<QLCEDataEntry> convertedData = new ArrayList<>();
    data.forEach(entry
        -> convertedData.add(
            QLCEDataEntry.builder()
                .cluster(entry.getClusterName())
                .clusterId(entry.getClusterId())
                .k8s(QLCEK8sEntity.builder()
                         .namespace(entry.getNamespace())
                         .workload(entry.getWorkload())
                         .node(entry.getNode())
                         .entityDetails(QLCEK8sEntityDetails.builder()
                                            .name(entry.getName())
                                            .id(entry.getId())
                                            .nodePoolName(entry.getNodePoolName())
                                            .podCapacity(entry.getPodCapacity())
                                            .instanceCategory(entry.getInstanceCategory())
                                            .machineType(entry.getMachineType())
                                            .qosClass(entry.getQosClass())
                                            .totalCost(entry.getTotalCost())
                                            .idleCost(entry.getIdleCost())
                                            .unallocatedCost(entry.getUnallocatedCost())
                                            .systemCost(entry.getSystemCost())
                                            .networkCost(entry.getNetworkCost())
                                            .storageTotalCost(entry.getStorageCost())
                                            .storageIdleCost(entry.getStorageActualIdleCost())
                                            .storageUnallocatedCost(entry.getStorageUnallocatedCost())
                                            .storageUtilizationValue(entry.getStorageUtilizationValue())
                                            .storageRequest(entry.getStorageRequest())
                                            .memoryTotalCost(entry.getMemoryBillingAmount())
                                            .memoryIdleCost(entry.getMemoryIdleCost())
                                            .memoryUnallocatedCost(entry.getMemoryUnallocatedCost())
                                            .memoryAllocatable(entry.getMemoryAllocatable())
                                            .memoryRequested(entry.getMemoryRequested())
                                            .cpuTotalCost(entry.getTotalCost())
                                            .cpuIdleCost(entry.getIdleCost())
                                            .cpuUnallocatedCost(entry.getUnallocatedCost())
                                            .cpuAllocatable(entry.getCpuAllocatable())
                                            .cpuRequested(entry.getCpuRequested())
                                            .createTime(entry.getCreateTime())
                                            .deleteTime(entry.getDeleteTime())
                                            .build())
                         .build())
                .build()));
    return QLCEData.builder().data(convertedData).build();
  }
}
