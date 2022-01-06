/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.function.Function.identity;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.commons.entities.k8s.K8sWorkload;

import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.graphql.datafetcher.billing.BillingDataHelper;
import software.wings.graphql.datafetcher.billing.BillingDataTableSchema.BillingDataTableKeys;
import software.wings.graphql.datafetcher.billing.BillingStatsDefaultKeys;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEData;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEDataEntry;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCESort;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCESortType;
import software.wings.graphql.datafetcher.k8sLabel.K8sLabelHelper;
import software.wings.graphql.schema.type.aggregation.LabelAggregation;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.TagAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortType;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableData;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableListData;
import software.wings.service.intfc.HarnessTagService;

import com.google.api.client.util.ArrayMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public abstract class AbstractStatsDataFetcherWithAggregationListAndTags<A, F, G, S, E, TA extends TagAggregation, LA
                                                                             extends LabelAggregation, EA>
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<A, F, G, S> {
  @Inject protected HarnessTagService tagService;
  @Inject protected K8sLabelHelper k8sLabelHelper;
  @Inject protected BillingDataHelper billingDataHelper;
  private static final String TYPE_LABEL = "K8sLabel";
  private static final String TYPE_UTILIZATION = "Utilization";

  protected abstract TA getTagAggregation(G groupBy);

  protected abstract LA getLabelAggregation(G groupBy);

  protected abstract EA getEntityAggregation(G groupBy);

  protected abstract EntityType getEntityType(E entityType);

  @Override
  public QLData postFetch(String accountId, List<G> groupByList, List<A> aggregateFunctions, List<S> sortCriteria,
      QLData qlData, Integer limit, boolean includeOthers) {
    List<TA> groupByTagList = getGroupByTag(groupByList);
    List<LA> groupByLabelList = getGroupByLabel(groupByList);
    if (!groupByTagList.isEmpty()) {
      TA groupByTagLevel1 = groupByTagList.get(0);

      if (qlData instanceof QLBillingStackedTimeSeriesData) {
        QLBillingStackedTimeSeriesData billingStackedTimeSeriesData = (QLBillingStackedTimeSeriesData) qlData;
        List<QLBillingStackedTimeSeriesDataPoint> billingStackedTimeSeriesDataPoints =
            billingStackedTimeSeriesData.getData();
        if (isEmpty(billingStackedTimeSeriesDataPoints)) {
          return qlData;
        }
        billingStackedTimeSeriesDataPoints.forEach(billingStackedTimeSeriesDataPoint -> {
          List<QLBillingDataPoint> dataPoints = getTagDataPoints(
              accountId, billingStackedTimeSeriesDataPoint.getValues(), groupByTagLevel1, includeOthers);
          billingStackedTimeSeriesDataPoint.setValues(dataPoints);
        });
      } else if (qlData instanceof QLEntityTableListData) {
        QLEntityTableListData entityTableListData = (QLEntityTableListData) qlData;
        List<QLEntityTableData> entityTableDataPoints = entityTableListData.getData();
        if (isEmpty(entityTableDataPoints)) {
          return qlData;
        }
        getTagEntityTableDataPoints(accountId, entityTableDataPoints, groupByTagLevel1, includeOthers);
      } else if (qlData instanceof QLCEData) {
        QLCEData data = (QLCEData) qlData;
        List<QLCEDataEntry> dataPoints = data.getData();
        if (isEmpty(dataPoints)) {
          return qlData;
        }
      }
    } else if (!groupByLabelList.isEmpty()) {
      LA groupByLabelLevel1 = groupByLabelList.get(0);
      prepareDataAfterLabelGroupBy(
          accountId, qlData, aggregateFunctions, groupByLabelLevel1, sortCriteria, includeOthers);
    }
    return qlData;
  }

  private void prepareDataAfterLabelGroupBy(String accountId, QLData qlData, List<A> aggregateFunctions,
      LA groupByLabelLevel1, List<S> sortCriteria, boolean includeOthers) {
    if (qlData instanceof QLBillingStackedTimeSeriesData) {
      List<QLCCMAggregationFunction> billingDataAggregations = (List<QLCCMAggregationFunction>) aggregateFunctions;
      QLBillingStackedTimeSeriesData billingStackedTimeSeriesData = (QLBillingStackedTimeSeriesData) qlData;
      prepareStackedTimeSeriesDataAfterLabelGroupBy(
          accountId, billingStackedTimeSeriesData, billingDataAggregations, groupByLabelLevel1, includeOthers);
    } else if (qlData instanceof QLEntityTableListData) {
      QLEntityTableListData entityTableListData = (QLEntityTableListData) qlData;
      List<QLEntityTableData> entityTableDataPoints = entityTableListData.getData();
      if (entityTableDataPoints != null) {
        getLabelEntityTableDataPoints(accountId, entityTableDataPoints, groupByLabelLevel1, includeOthers);
        sortEntityTableData(entityTableDataPoints, (List<QLBillingSortCriteria>) sortCriteria);
      }
    } else if (qlData instanceof QLCEData) {
      QLCEData data = (QLCEData) qlData;
      List<QLCEDataEntry> dataPoints = data.getData();
      getLabelCeExportDataPoints(accountId, dataPoints, groupByLabelLevel1);
      sortCEExportData(dataPoints, (List<QLCESort>) sortCriteria);
    }
  }

  private void prepareStackedTimeSeriesDataAfterLabelGroupBy(String accountId,
      QLBillingStackedTimeSeriesData billingStackedTimeSeriesData,
      List<QLCCMAggregationFunction> billingDataAggregations, LA groupByLabelLevel1, boolean includeOthers) {
    billingDataAggregations.forEach(aggregateFunction -> {
      List<QLBillingStackedTimeSeriesDataPoint> billingStackedTimeSeriesDataPoints = new ArrayList<>();
      if (aggregateFunction.getColumnName().equalsIgnoreCase(BillingDataTableKeys.billingAmount)
          || aggregateFunction.getColumnName().equalsIgnoreCase(BillingDataTableKeys.idleCost)) {
        billingStackedTimeSeriesDataPoints = billingStackedTimeSeriesData.getData();
      } else if (aggregateFunction.getColumnName().equalsIgnoreCase(BillingDataTableKeys.cpuIdleCost)) {
        billingStackedTimeSeriesDataPoints = billingStackedTimeSeriesData.getCpuIdleCost();
      } else if (aggregateFunction.getColumnName().equalsIgnoreCase(BillingDataTableKeys.memoryIdleCost)) {
        billingStackedTimeSeriesDataPoints = billingStackedTimeSeriesData.getMemoryIdleCost();
      } else if (aggregateFunction.getColumnName().equalsIgnoreCase(BillingDataTableKeys.maxCpuUtilization)
          || aggregateFunction.getColumnName().equalsIgnoreCase(BillingDataTableKeys.avgCpuUtilization)) {
        billingStackedTimeSeriesDataPoints = billingStackedTimeSeriesData.getCpuUtilMetrics();
      } else if (aggregateFunction.getColumnName().equalsIgnoreCase(BillingDataTableKeys.maxMemoryUtilization)
          || aggregateFunction.getColumnName().equalsIgnoreCase(BillingDataTableKeys.avgMemoryUtilization)) {
        billingStackedTimeSeriesDataPoints = billingStackedTimeSeriesData.getMemoryUtilMetrics();
      }
      if (billingStackedTimeSeriesDataPoints != null) {
        billingStackedTimeSeriesDataPoints.forEach(billingStackedTimeSeriesDataPoint -> {
          List<QLBillingDataPoint> dataPoints =
              getLabelDataPoints(accountId, billingStackedTimeSeriesDataPoint.getValues(), groupByLabelLevel1,
                  aggregateFunction.getOperationType(), includeOthers);
          billingStackedTimeSeriesDataPoint.setValues(dataPoints);
        });
      }
    });
  }

  protected int findFirstGroupByTagPosition(List<G> groupByList) {
    int index = -1;
    for (G groupByEntry : groupByList) {
      index++;
      TA tagAggregation = getTagAggregation(groupByEntry);
      if (tagAggregation != null) {
        return index;
      }
    }
    return -1;
  }

  protected int findFirstGroupByLabelPosition(List<G> groupByList) {
    int index = -1;
    for (G groupByEntry : groupByList) {
      index++;
      LA labelAggregation = getLabelAggregation(groupByEntry);
      if (labelAggregation != null) {
        return index;
      }
    }
    return -1;
  }

  protected List<TA> getGroupByTag(List<G> groupByList) {
    List<TA> groupByTagList = new ArrayList<>();
    if (isEmpty(groupByList)) {
      return groupByTagList;
    }

    for (G groupBy : groupByList) {
      TA tagAggregation = getTagAggregation(groupBy);
      if (tagAggregation != null) {
        groupByTagList.add(tagAggregation);
      }
    }
    return groupByTagList;
  }

  protected List<LA> getGroupByLabel(List<G> groupByList) {
    List<LA> groupByLabelList = new ArrayList<>();
    if (isEmpty(groupByList)) {
      return groupByLabelList;
    }

    for (G groupBy : groupByList) {
      LA labelAggregation = getLabelAggregation(groupBy);
      if (labelAggregation != null) {
        groupByLabelList.add(labelAggregation);
      }
    }
    return groupByLabelList;
  }

  protected int findFirstGroupByEntityPosition(List<G> groupByList) {
    int index = -1;
    for (G groupByEntry : groupByList) {
      index++;
      EA entityAggregation = getEntityAggregation(groupByEntry);
      if (entityAggregation != null) {
        return index;
      }
    }
    return -1;
  }

  protected List<EA> getGroupByEntityListFromTags(
      List<G> groupByList, List<EA> groupByEntityList, List<TA> groupByTagList) {
    if (!groupByTagList.isEmpty()) {
      if (groupByEntityList == null) {
        groupByEntityList = new ArrayList<>();
      }
      // We need to determine the order so that the correct grouping happens at each level.
      if (!groupByEntityList.isEmpty() && groupByTagList.size() == 1) {
        int entityIndex = findFirstGroupByEntityPosition(groupByList);
        int tagIndex = findFirstGroupByTagPosition(groupByList);
        if (entityIndex > tagIndex) {
          groupByEntityList.add(0, getGroupByEntityFromTag(groupByTagList.get(0)));
        } else {
          groupByEntityList.add(getGroupByEntityFromTag(groupByTagList.get(0)));
        }
      } else {
        for (TA groupByTagEntry : groupByTagList) {
          groupByEntityList.add(getGroupByEntityFromTag(groupByTagEntry));
        }
      }
    }
    return groupByEntityList;
  }

  protected List<EA> getGroupByEntityListFromLabels(
      List<G> groupByList, List<EA> groupByEntityList, List<LA> groupByLabelList) {
    if (!groupByLabelList.isEmpty()) {
      if (groupByEntityList == null) {
        groupByEntityList = new ArrayList<>();
      }
      // We need to determine the order so that the correct grouping happens at each level.
      if (!groupByEntityList.isEmpty() && groupByLabelList.size() == 1) {
        int entityIndex = findFirstGroupByEntityPosition(groupByList);
        int labelIndex = findFirstGroupByLabelPosition(groupByList);
        if (entityIndex > labelIndex) {
          groupByEntityList.add(0, getGroupByEntityFromLabel(groupByLabelList.get(0)));
        } else {
          groupByEntityList.add(getGroupByEntityFromLabel(groupByLabelList.get(0)));
        }
      } else {
        for (LA groupByLabelEntry : groupByLabelList) {
          groupByEntityList.add(getGroupByEntityFromLabel(groupByLabelEntry));
        }
      }
    }
    return groupByEntityList;
  }

  private List<QLBillingDataPoint> getTagDataPoints(
      String accountId, List<QLBillingDataPoint> dataPoints, TA groupByTag, boolean includeOthers) {
    Set<String> entityIdSet =
        dataPoints.stream().map(dataPoint -> dataPoint.getKey().getId()).collect(Collectors.toSet());
    Set<HarnessTagLink> tagLinks = tagService.getTagLinks(
        accountId, getEntityType((E) groupByTag.getEntityType()), entityIdSet, groupByTag.getTagName());
    Map<String, HarnessTagLink> entityIdTagLinkMap =
        tagLinks.stream().collect(Collectors.toMap(HarnessTagLink::getEntityId, identity()));

    ArrayMap<String, QLBillingDataPoint> tagNameDataPointMap = new ArrayMap<>();

    dataPoints.removeIf(dataPoint -> {
      String entityId = dataPoint.getKey().getId();
      HarnessTagLink tagLink = entityIdTagLinkMap.get(entityId);
      String tagName;
      if (tagLink == null) {
        if (!includeOthers) {
          return true;
        }
        tagName = BillingStatsDefaultKeys.DEFAULT_TAG;
      } else {
        tagName = tagLink.getKey() + ":" + tagLink.getValue();
      }
      QLBillingDataPoint existingDataPoint = tagNameDataPointMap.get(tagName);
      if (existingDataPoint != null) {
        existingDataPoint.setValue(billingDataHelper.getRoundedDoubleValue(
            existingDataPoint.getValue().doubleValue() + dataPoint.getValue().doubleValue()));
        return true;
      }

      QLReference tagRef = QLReference.builder().id(tagName).name(tagName).type(EntityType.TAG.name()).build();
      dataPoint.setKey(tagRef);
      tagNameDataPointMap.put(tagName, dataPoint);
      return false;
    });
    return new ArrayList<>(tagNameDataPointMap.values());
  }

  private List<QLBillingDataPoint> getLabelDataPoints(String accountId, List<QLBillingDataPoint> dataPoints,
      LA groupByLabel, QLCCMAggregateOperation operation, boolean includeOthers) {
    Set<String> entityIdSet = dataPoints.stream()
                                  .map(dataPoint -> getWorkloadNameFromId(dataPoint.getKey().getId()))
                                  .collect(Collectors.toSet());
    Set<K8sWorkload> labelLinks = k8sLabelHelper.getLabelLinks(accountId, entityIdSet, groupByLabel.getName());
    Map<String, K8sWorkload> entityIdLabelLinkMap = labelLinks.stream().collect(Collectors.toMap(k8sWorkload
        -> k8sWorkload.getClusterId() + BillingStatsDefaultKeys.TOKEN + k8sWorkload.getNamespace()
            + BillingStatsDefaultKeys.TOKEN + k8sWorkload.getName(),
        identity()));

    ArrayMap<String, QLBillingDataPoint> labelNameDataPointMap = new ArrayMap<>();
    ArrayMap<String, Long> numberOfDataPoints = new ArrayMap<>();
    String labelName = groupByLabel.getName();
    dataPoints.removeIf(dataPoint -> {
      String entityId = dataPoint.getKey().getId();
      if (dataPoint.getKey().getType().equals(TYPE_LABEL)
          || (dataPoint.getKey().getType().equals(TYPE_UTILIZATION)
              && !dataPoint.getKey().getName().equals(operation.name()))) {
        labelNameDataPointMap.put(entityId, dataPoint);
        return false;
      }
      K8sWorkload workload = entityIdLabelLinkMap.get(entityId);
      String label;
      if (workload == null) {
        if (!includeOthers) {
          return true;
        }
        label = BillingStatsDefaultKeys.DEFAULT_LABEL;
      } else {
        label = workload.getLabels().get(labelName);
      }
      return updateDataPointsForLabelAggregation(
          dataPoint, labelNameDataPointMap, numberOfDataPoints, label, operation);
    });

    labelNameDataPointMap.forEach((key, dataPoint) -> {
      if (key.contains("AVG")) {
        dataPoint.setValue(billingDataHelper.getRoundedDoubleValue(dataPoint.getValue().doubleValue()
            / numberOfDataPoints.get(dataPoint.getKey().getId() + operation.name())));
      }
    });

    return new ArrayList<>(labelNameDataPointMap.values());
  }

  private String getWorkloadNameFromId(String id) {
    String[] strArray = id.split(BillingStatsDefaultKeys.TOKEN);
    // Id has this format namespace:workloadName
    return strArray[strArray.length - 1];
  }

  private boolean updateDataPointsForLabelAggregation(QLBillingDataPoint dataPoint,
      ArrayMap<String, QLBillingDataPoint> labelNameDataPointMap, ArrayMap<String, Long> numberOfDataPoints,
      String label, QLCCMAggregateOperation operation) {
    String labelMapKey = label + operation.name();
    QLBillingDataPoint existingDataPoint = labelNameDataPointMap.get(labelMapKey);
    if (existingDataPoint != null) {
      switch (operation) {
        case SUM:
          existingDataPoint.setValue(billingDataHelper.getRoundedDoubleValue(
              existingDataPoint.getValue().doubleValue() + dataPoint.getValue().doubleValue()));
          break;
        case MAX:
          existingDataPoint.setValue(billingDataHelper.getRoundedDoubleValue(
              Math.max(existingDataPoint.getValue().doubleValue(), dataPoint.getValue().doubleValue())));
          break;
        case AVG:
          existingDataPoint.setValue(billingDataHelper.getRoundedDoubleValue(
              existingDataPoint.getValue().doubleValue() + dataPoint.getValue().doubleValue()));
          numberOfDataPoints.put(labelMapKey, numberOfDataPoints.get(labelMapKey) + 1);
          break;
        default:
          break;
      }
      return true;
    }

    String name = (operation == QLCCMAggregateOperation.MAX || operation == QLCCMAggregateOperation.AVG)
        ? operation.name()
        : label;
    QLReference labelRef = QLReference.builder().id(label).name(name).type(TYPE_LABEL).build();
    dataPoint.setKey(labelRef);
    labelNameDataPointMap.put(labelMapKey, dataPoint);
    if (operation == QLCCMAggregateOperation.AVG) {
      numberOfDataPoints.put(labelMapKey, 1L);
    }
    return false;
  }

  private void getTagEntityTableDataPoints(
      String accountId, List<QLEntityTableData> dataPoints, TA groupByTag, boolean includeOthers) {
    Set<String> entityIdSet = dataPoints.stream().map(dataPoint -> dataPoint.getId()).collect(Collectors.toSet());
    Set<HarnessTagLink> tagLinks = tagService.getTagLinks(
        accountId, getEntityType((E) groupByTag.getEntityType()), entityIdSet, groupByTag.getTagName());
    Map<String, HarnessTagLink> entityIdTagLinkMap =
        tagLinks.stream().collect(Collectors.toMap(HarnessTagLink::getEntityId, identity()));

    ArrayMap<String, QLEntityTableData> tagNameDataPointMap = new ArrayMap<>();
    ArrayMap<String, Double> aggregatedPrevBillingAmount = new ArrayMap<>();

    dataPoints.removeIf(dataPoint -> {
      String entityId = dataPoint.getId();
      HarnessTagLink tagLink = entityIdTagLinkMap.get(entityId);
      String tagName;
      if (tagLink == null) {
        if (!includeOthers) {
          return true;
        }
        tagName = BillingStatsDefaultKeys.DEFAULT_TAG;
      } else {
        tagName = tagLink.getKey() + ":" + tagLink.getValue();
      }
      QLEntityTableData existingDataPoint = tagNameDataPointMap.get(tagName);
      if (existingDataPoint != null) {
        existingDataPoint.setTotalCost(
            billingDataHelper.getRoundedDoubleValue(existingDataPoint.getTotalCost() + dataPoint.getTotalCost()));
        existingDataPoint.setIdleCost(
            billingDataHelper.getRoundedDoubleValue(existingDataPoint.getIdleCost() + dataPoint.getIdleCost()));
        existingDataPoint.setCpuIdleCost(
            billingDataHelper.getRoundedDoubleValue(existingDataPoint.getCpuIdleCost() + dataPoint.getCpuIdleCost()));
        existingDataPoint.setMemoryIdleCost(billingDataHelper.getRoundedDoubleValue(
            existingDataPoint.getMemoryIdleCost() + dataPoint.getMemoryIdleCost()));
        existingDataPoint.setCostTrend(billingDataHelper.getRoundedDoubleValue(existingDataPoint.getCostTrend()
            + billingDataHelper.getRoundedDoubleValue(dataPoint.getPrevBillingAmount() * dataPoint.getCostTrend())));
        aggregatedPrevBillingAmount.put(
            tagName, aggregatedPrevBillingAmount.get(tagName) + dataPoint.getPrevBillingAmount());
        return true;
      }

      dataPoint.setId(tagName);
      dataPoint.setName(tagName);
      dataPoint.setType(EntityType.TAG.name());
      dataPoint.setCostTrend(
          billingDataHelper.getRoundedDoubleValue(dataPoint.getPrevBillingAmount() * dataPoint.getCostTrend()));
      aggregatedPrevBillingAmount.put(tagName, dataPoint.getPrevBillingAmount());
      tagNameDataPointMap.put(tagName, dataPoint);
      return false;
    });

    tagNameDataPointMap.forEach(
        (key, dataPoint)
            -> dataPoint.setCostTrend(billingDataHelper.getRoundedDoubleValue(
                dataPoint.getCostTrend() / aggregatedPrevBillingAmount.get(dataPoint.getId()))));
  }

  private void getLabelEntityTableDataPoints(
      String accountId, List<QLEntityTableData> dataPoints, LA groupByLabel, boolean includeOthers) {
    if (dataPoints.isEmpty()) {
      return;
    }
    Set<String> entityIdSet = dataPoints.stream().map(QLEntityTableData::getWorkloadName).collect(Collectors.toSet());
    Set<K8sWorkload> labelLinks = k8sLabelHelper.getLabelLinks(accountId, entityIdSet, groupByLabel.getName());
    Map<String, K8sWorkload> entityIdLabelLinkMap = labelLinks.stream().collect(Collectors.toMap(k8sWorkload
        -> k8sWorkload.getClusterId() + BillingStatsDefaultKeys.TOKEN + k8sWorkload.getNamespace()
            + BillingStatsDefaultKeys.TOKEN + k8sWorkload.getName(),
        identity()));

    ArrayMap<String, QLEntityTableData> labelNameDataPointMap = new ArrayMap<>();
    ArrayMap<String, Long> numberOfDataPoints = new ArrayMap<>();
    ArrayMap<String, Double> aggregatedPrevBillingAmount = new ArrayMap<>();
    String labelName = groupByLabel.getName();
    dataPoints.removeIf(dataPoint -> {
      String entityId = dataPoint.getId();
      K8sWorkload workload = entityIdLabelLinkMap.get(entityId);
      String label;
      if (workload == null) {
        if (!includeOthers) {
          return true;
        }
        label = BillingStatsDefaultKeys.DEFAULT_LABEL;
      } else {
        label = workload.getLabels().get(labelName);
      }

      QLEntityTableData existingDataPoint = labelNameDataPointMap.get(label);
      if (existingDataPoint != null) {
        existingDataPoint.setTotalCost(
            billingDataHelper.getRoundedDoubleValue(existingDataPoint.getTotalCost() + dataPoint.getTotalCost()));
        existingDataPoint.setIdleCost(
            billingDataHelper.getRoundedDoubleValue(existingDataPoint.getIdleCost() + dataPoint.getIdleCost()));
        existingDataPoint.setCpuIdleCost(
            billingDataHelper.getRoundedDoubleValue(existingDataPoint.getCpuIdleCost() + dataPoint.getCpuIdleCost()));
        existingDataPoint.setMemoryIdleCost(billingDataHelper.getRoundedDoubleValue(
            existingDataPoint.getMemoryIdleCost() + dataPoint.getMemoryIdleCost()));
        existingDataPoint.setMaxCpuUtilization(
            Math.max(existingDataPoint.getMaxCpuUtilization(), dataPoint.getMaxCpuUtilization()));
        existingDataPoint.setMaxMemoryUtilization(
            Math.max(existingDataPoint.getMaxMemoryUtilization(), dataPoint.getMaxMemoryUtilization()));
        existingDataPoint.setAvgCpuUtilization(
            existingDataPoint.getAvgCpuUtilization() + dataPoint.getAvgCpuUtilization());
        existingDataPoint.setAvgMemoryUtilization(
            existingDataPoint.getAvgMemoryUtilization() + dataPoint.getAvgMemoryUtilization());
        numberOfDataPoints.put(label, numberOfDataPoints.get(label) + 1);
        existingDataPoint.setCostTrend(billingDataHelper.getRoundedDoubleValue(existingDataPoint.getCostTrend()
            + billingDataHelper.getRoundedDoubleValue(dataPoint.getPrevBillingAmount() * dataPoint.getCostTrend())));
        aggregatedPrevBillingAmount.put(
            label, aggregatedPrevBillingAmount.get(label) + dataPoint.getPrevBillingAmount());
        return true;
      }

      dataPoint.setId(label);
      dataPoint.setName(label);
      dataPoint.setType(TYPE_LABEL);
      dataPoint.setLabel(label);
      dataPoint.setCostTrend(
          billingDataHelper.getRoundedDoubleValue(dataPoint.getPrevBillingAmount() * dataPoint.getCostTrend()));
      aggregatedPrevBillingAmount.put(label, dataPoint.getPrevBillingAmount());
      numberOfDataPoints.put(label, 1L);
      labelNameDataPointMap.put(label, dataPoint);
      return false;
    });

    labelNameDataPointMap.forEach((key, dataPoint) -> {
      dataPoint.setAvgCpuUtilization(billingDataHelper.getRoundedDoubleValue(
          dataPoint.getAvgCpuUtilization() / numberOfDataPoints.get(dataPoint.getId())));
      dataPoint.setAvgMemoryUtilization(billingDataHelper.getRoundedDoubleValue(
          dataPoint.getAvgMemoryUtilization() / numberOfDataPoints.get(dataPoint.getId())));
      dataPoint.setCostTrend(billingDataHelper.getRoundedDoubleValue(
          dataPoint.getCostTrend() / aggregatedPrevBillingAmount.get(dataPoint.getId())));
    });
  }

  private void sortEntityTableData(List<QLEntityTableData> dataPoints, List<QLBillingSortCriteria> sortCriteria) {
    if (sortCriteria != null && sortCriteria.isEmpty()) {
      sortCriteria.forEach(sort -> {
        Comparator<QLEntityTableData> comparator;
        if (sort.getSortType() == QLBillingSortType.Amount) {
          comparator = Comparator.comparing(QLEntityTableData::getTotalCost);
        } else if (sort.getSortType() == QLBillingSortType.IdleCost) {
          comparator = Comparator.comparing(QLEntityTableData::getIdleCost);
        } else {
          return;
        }

        if (sort.getSortOrder() == QLSortOrder.ASCENDING) {
          Collections.sort(dataPoints, comparator);
        } else {
          Collections.sort(dataPoints, comparator.reversed());
        }
      });
    }
  }

  private void getLabelCeExportDataPoints(String accountId, List<QLCEDataEntry> dataPoints, LA groupByLabel) {
    if (dataPoints.isEmpty()) {
      return;
    }
    Set<String> entityIdSet =
        dataPoints.stream().map(dataPoint -> dataPoint.getK8s().getWorkload()).collect(Collectors.toSet());
    Set<K8sWorkload> labelLinks = k8sLabelHelper.getLabelLinks(accountId, entityIdSet, groupByLabel.getName());
    Map<String, K8sWorkload> entityIdLabelLinkMap =
        labelLinks.stream().collect(Collectors.toMap(K8sWorkload::getName, identity()));

    ArrayMap<String, QLCEDataEntry> labelNameDataPointMap = new ArrayMap<>();
    String labelName = groupByLabel.getName();
    dataPoints.removeIf(dataPoint -> {
      String entityId = dataPoint.getK8s().getWorkload();
      K8sWorkload workload = entityIdLabelLinkMap.get(entityId);
      String label;
      if (workload == null) {
        label = BillingStatsDefaultKeys.DEFAULT_LABEL;
      } else {
        label = workload.getLabels().get(labelName);
      }

      QLCEDataEntry existingDataPoint = labelNameDataPointMap.get(label);
      if (existingDataPoint != null) {
        if (existingDataPoint.getTotalCost() != null) {
          existingDataPoint.setTotalCost(
              billingDataHelper.getRoundedDoubleValue(existingDataPoint.getTotalCost() + dataPoint.getTotalCost()));
        }
        if (existingDataPoint.getIdleCost() != null) {
          existingDataPoint.setIdleCost(
              billingDataHelper.getRoundedDoubleValue(existingDataPoint.getIdleCost() + dataPoint.getIdleCost()));
        }
        if (existingDataPoint.getUnallocatedCost() != null) {
          existingDataPoint.setUnallocatedCost(billingDataHelper.getRoundedDoubleValue(
              existingDataPoint.getUnallocatedCost() + dataPoint.getUnallocatedCost()));
        }
        return true;
      }

      dataPoint.setLabelName(labelName);
      dataPoint.setLabelValue(label);
      labelNameDataPointMap.put(label, dataPoint);
      return false;
    });
  }

  private void sortCEExportData(List<QLCEDataEntry> dataPoints, List<QLCESort> sortCriteria) {
    if (sortCriteria != null && sortCriteria.isEmpty()) {
      sortCriteria.forEach(sort -> {
        Comparator<QLCEDataEntry> comparator;
        if (sort.getSortType() == QLCESortType.TOTALCOST) {
          comparator = Comparator.comparing(QLCEDataEntry::getTotalCost);
        } else if (sort.getSortType() == QLCESortType.IDLECOST) {
          comparator = Comparator.comparing(QLCEDataEntry::getIdleCost);
        } else if (sort.getSortType() == QLCESortType.UNALLOCATEDCOST) {
          comparator = Comparator.comparing(QLCEDataEntry::getIdleCost);
        } else {
          return;
        }

        if (sort.getOrder() == QLSortOrder.ASCENDING) {
          Collections.sort(dataPoints, comparator);
        } else {
          Collections.sort(dataPoints, comparator.reversed());
        }
      });
    }
  }

  protected abstract EA getGroupByEntityFromTag(TA groupByTag);

  protected abstract EA getGroupByEntityFromLabel(LA groupByLabel);
}
