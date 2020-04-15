package software.wings.graphql.datafetcher.billing;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo.QLStatsBreakdownInfoBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstChartData;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstChartDataPoint;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstChartDataPoint.QLSunburstChartDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstGridDataPoint;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstGridDataPoint.QLSunburstGridDataPointBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Slf4j
public class SunburstChartStatsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<QLCCMAggregationFunction, QLBillingDataFilter,
        QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject QLBillingStatsHelper billingStatsHelper;
  @Inject BillingDataHelper billingDataHelper;

  private static final String ROOT_PARENT_ID = "ROOT_PARENT_ID";
  private static int idleCostBaseline = 30;
  private static int unallocatedCostBaseline = 5;
  private static String unsupportedType = "UnsupportedType ";
  private static final String OTHERS = "Others";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort, Integer limit,
      Integer offset) {
    try {
      if (timeScaleDBService.isValid()) {
        List<QLSunburstGridDataPoint> sunburstGridDataPointList =
            getGridDataPointsForAllViews(accountId, aggregateFunction, filters, groupBy, sort);
        List<QLSunburstChartDataPoint> sunburstChartDataPointList = getSunburstChartData(accountId, aggregateFunction,
            filters, groupBy, sort, true, convertGridPointsToMap(sunburstGridDataPointList));

        return QLSunburstChartData.builder()
            .data(sunburstChartDataPointList)
            .gridData(sunburstGridDataPointList)
            .build();
      } else {
        throw new InvalidRequestException("Cannot process request in BillingStatsFilterValuesDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  private List<QLSunburstGridDataPoint> getGridDataPointsForAllViews(String accountId,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy,
      List<QLBillingSortCriteria> sort) {
    List<QLSunburstGridDataPoint> sunburstGridDataPointList = new ArrayList<>();
    for (QLCCMGroupBy groupByPoint : groupBy) {
      boolean isClusterGroupBy = false;
      if (groupByPoint.getEntityGroupBy() == QLCCMEntityGroupBy.Cluster) {
        isClusterGroupBy = true;
      }
      sunburstGridDataPointList.addAll(getSunburstGridData(
          accountId, aggregateFunction, new ArrayList<>(filters), Arrays.asList(groupByPoint), sort, isClusterGroupBy));
    }
    return sunburstGridDataPointList;
  }

  private Map<String, QLSunburstGridDataPoint> convertGridPointsToMap(
      List<QLSunburstGridDataPoint> sunburstGridDataPointList) {
    Map<String, QLSunburstGridDataPoint> gridDataPointHashMap = new HashMap<>();
    for (QLSunburstGridDataPoint sunburstGridDataPoint : sunburstGridDataPointList) {
      gridDataPointHashMap.put(sunburstGridDataPoint.getId(), sunburstGridDataPoint);
    }
    return gridDataPointHashMap;
  }

  @VisibleForTesting
  List<QLSunburstGridDataPoint> getSunburstGridData(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort,
      boolean isClusterGroupBy) {
    Map<String, Double> unallocatedCostMap = new HashMap<>();

    if (isClusterGroupBy) {
      unallocatedCostMap = getUnallocatedCostData(accountId, filters, groupBy, sort);
    }

    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupBy);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupBy);

    queryData = billingDataQueryBuilder.formQuery(accountId, filters, aggregateFunction,
        groupByEntityList.isEmpty() ? Collections.emptyList() : groupByEntityList, groupByTime, sort, isClusterGroupBy,
        true);
    logger.info("getSunburstGridData query: {}", queryData.getQuery());

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            accountId, aggregateFunction, filters, groupByEntityList, groupByTime, sort);

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateSunburstGridData(queryData, resultSet, unallocatedCostMap, isClusterGroupBy,
          groupByEntityList.size(), entityIdToPrevBillingAmountData, filters);
    } catch (SQLException e) {
      logger.error("SunburstChartStatsDataFetcher (getSunburstGridData) Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return Collections.emptyList();
  }

  private List<QLSunburstGridDataPoint> generateSunburstGridData(BillingDataQueryMetadata queryData,
      ResultSet resultSet, Map<String, Double> unallocatedCostMap, boolean isClusterGroupBy, int groupBySize,
      Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData, List<QLBillingDataFilter> filters)
      throws SQLException {
    List<QLSunburstGridDataPoint> sunburstGridDataPointList = new ArrayList<>();
    Double costTrend = BillingStatsDefaultKeys.COSTTREND;
    while (null != resultSet && resultSet.next()) {
      QLSunburstGridDataPointBuilder gridDataPointBuilder = QLSunburstGridDataPoint.builder();
      QLStatsBreakdownInfoBuilder qlStatsBreakdownInfoBuilder = QLStatsBreakdownInfo.builder();
      for (BillingDataQueryMetadata.BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            switch (field) {
              case SUM:
                qlStatsBreakdownInfoBuilder.total(billingDataHelper.roundingDoubleFieldValue(field, resultSet));
                break;
              case IDLECOST:
                qlStatsBreakdownInfoBuilder.idle(billingDataHelper.roundingDoubleFieldValue(field, resultSet));
                break;
              default:
                throw new InvalidRequestException(unsupportedType + field.getDataType());
            }
            break;
          case STRING:
            switch (field) {
              case CLUSTERID:
              case APPID:
                String fieldName = field.getFieldName();
                String entityId = resultSet.getString(fieldName);
                qlStatsBreakdownInfoBuilder.unallocated(
                    unallocatedCostMap.get(entityId) != null ? unallocatedCostMap.get(entityId) : 0);
                gridDataPointBuilder.id(entityId);
                gridDataPointBuilder.type(fieldName);
                gridDataPointBuilder.name(billingStatsHelper.getEntityName(field, entityId));
                break;
              default:
                throw new InvalidRequestException(unsupportedType + field.getDataType());
            }
            break;
          default:
            break;
        }
      }
      QLStatsBreakdownInfo breakdownInfo = qlStatsBreakdownInfoBuilder.build();
      validateBreakdownInfo(breakdownInfo, isClusterGroupBy);
      gridDataPointBuilder.efficiencyScore(calculateEfficiencyScore(breakdownInfo, isClusterGroupBy));
      gridDataPointBuilder.value(breakdownInfo.getTotal());
      gridDataPointBuilder.trend(costTrend);
      QLSunburstGridDataPoint sunburstGridDataPoint = gridDataPointBuilder.build();
      String id = sunburstGridDataPoint.getId();
      if (entityIdToPrevBillingAmountData != null && entityIdToPrevBillingAmountData.containsKey(id)) {
        sunburstGridDataPoint.setTrend(
            billingDataHelper.getCostTrendForEntity(resultSet, entityIdToPrevBillingAmountData.get(id), filters));
      }

      sunburstGridDataPointList.add(sunburstGridDataPoint);
    }
    return sunburstGridDataPointList;
  }

  private void validateBreakdownInfo(QLStatsBreakdownInfo breakdownInfo, boolean isClusterGroupBy) {
    if (breakdownInfo.getIdle() == null) {
      breakdownInfo.setIdle(0.0);
    }

    if (breakdownInfo.getTotal() == null) {
      breakdownInfo.setTotal(0.0);
    }

    if (breakdownInfo.getUnallocated() == null) {
      breakdownInfo.setUnallocated(0.0);
    }
    if (isClusterGroupBy && !breakdownInfo.getIdle().equals(0.0)) {
      breakdownInfo.setIdle(breakdownInfo.getIdle().doubleValue() - breakdownInfo.getUnallocated().doubleValue());
    }
  }

  private int calculateEfficiencyScore(QLStatsBreakdownInfo costStats, boolean isClusterGroupBy) {
    int utilizedBaseline = 100 - idleCostBaseline;
    if (isClusterGroupBy) {
      utilizedBaseline -= unallocatedCostBaseline;
    }
    double utilized = costStats.getTotal().doubleValue() - costStats.getIdle().doubleValue()
        - costStats.getUnallocated().doubleValue();
    double total = costStats.getTotal().doubleValue();
    double utilizedPercentage = utilized / total * 100;
    int efficiencyScore = (int) ((1 - ((utilizedBaseline - utilizedPercentage) / utilizedBaseline)) * 100);
    return efficiencyScore > 100 ? 100 : efficiencyScore;
  }

  @VisibleForTesting
  List<QLSunburstChartDataPoint> getSunburstChartData(String accountId,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy,
      List<QLBillingSortCriteria> sort, boolean addRootParent,
      Map<String, QLSunburstGridDataPoint> sunburstGridDataPointMap) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupBy);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupBy);
    queryData = billingDataQueryBuilder.formQuery(
        accountId, filters, aggregateFunction, groupByEntityList, groupByTime, sort, false);
    logger.info("getSunburstChartData query: {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());

      return generateSunburstChartData(
          queryData, resultSet, addRootParent, getContextName(accountId), sunburstGridDataPointMap);
    } catch (SQLException e) {
      logger.error("SunburstChartStatsDataFetcher (getSunburstChartData) Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return Collections.emptyList();
  }

  private String getContextName(String accountId) {
    return wingsPersistence.get(Account.class, accountId) != null
        ? wingsPersistence.get(Account.class, accountId).getAccountName()
        : accountId;
  }

  private List<QLSunburstChartDataPoint> generateSunburstChartData(BillingDataQueryMetadata queryData,
      ResultSet resultSet, boolean addRootParent, String contextName,
      Map<String, QLSunburstGridDataPoint> sunburstGridDataPointMap) throws SQLException {
    List<BillingDataQueryMetadata.BillingDataMetaDataFields> groupByFields = queryData.getGroupByFields();
    BillingDataQueryMetadata.BillingDataMetaDataFields parentField = groupByFields.get(0);

    List<QLSunburstChartDataPoint> sunburstChartDataPoints = new ArrayList<>();
    addRootParentIdIfSpecified(addRootParent, sunburstChartDataPoints, contextName);

    while (resultSet != null && resultSet.next()) {
      QLSunburstChartDataPointBuilder dataPointBuilder = QLSunburstChartDataPoint.builder();
      String parentFieldName = resultSet.getString(parentField.getFieldName());
      QLSunburstGridDataPoint metadata = sunburstGridDataPointMap.get(parentFieldName);
      dataPointBuilder.id(parentFieldName);
      dataPointBuilder.metadata(metadata);
      double value =
          resultSet.getBigDecimal(BillingDataQueryMetadata.BillingDataMetaDataFields.SUM.getFieldName()).doubleValue();
      dataPointBuilder.value(billingDataHelper.getRoundedDoubleValue(value));
      dataPointBuilder.name(billingStatsHelper.getEntityName(parentField, parentFieldName));
      dataPointBuilder.type(parentField.getFieldName());
      dataPointBuilder.parent(ROOT_PARENT_ID);
      sunburstChartDataPoints.add(dataPointBuilder.build());
    }

    return sunburstChartDataPoints;
  }

  private void addRootParentIdIfSpecified(
      boolean addRootParent, List<QLSunburstChartDataPoint> sunburstChartDataPoints, String contextName) {
    final String ROOT_PARENT_CONSTANT = "";
    if (addRootParent) {
      sunburstChartDataPoints.add(
          QLSunburstChartDataPoint.builder().id(ROOT_PARENT_ID).name(contextName).parent(ROOT_PARENT_CONSTANT).build());
    }
  }

  @VisibleForTesting
  Map<String, Double> getUnallocatedCostData(String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupBy);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupBy);

    List<QLBillingDataFilter> filtersWithUnallocatedFilter = new ArrayList<>(filters);
    addUnallocatedInstanceFilter(filtersWithUnallocatedFilter);

    List<QLCCMAggregationFunction> aggregateFunction = getBillingAggregation();

    queryData = billingDataQueryBuilder.formQuery(accountId, filtersWithUnallocatedFilter, aggregateFunction,
        groupByEntityList.isEmpty() ? Collections.emptyList() : Collections.singletonList(groupByEntityList.get(0)),
        groupByTime, sort, false);
    logger.info("getUnallocatedCostData query: {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateUnallocatedData(queryData, resultSet);
    } catch (SQLException e) {
      logger.error("SunburstChartStatsDataFetcher (getUnallocatedCostData) Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return new HashMap<>();
  }

  Map<String, Double> generateUnallocatedData(BillingDataQueryMetadata queryData, ResultSet resultSet)
      throws SQLException {
    Map<String, Double> unallocatedCostMap = new HashMap<>();
    while (null != resultSet && resultSet.next()) {
      String entityId = "defaultEntityId";
      double unallocatedCost = 0;
      for (BillingDataQueryMetadata.BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            unallocatedCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case STRING:
            entityId = resultSet.getString(field.getFieldName());
            break;
          default:
            throw new InvalidRequestException(unsupportedType + field.getDataType());
        }
      }
      unallocatedCostMap.put(entityId, unallocatedCost);
    }

    return unallocatedCostMap;
  }

  private List<QLCCMAggregationFunction> getBillingAggregation() {
    return Collections.singletonList(QLCCMAggregationFunction.builder()
                                         .operationType(QLCCMAggregateOperation.SUM)
                                         .columnName("billingamount")
                                         .build());
  }

  private void addUnallocatedInstanceFilter(List<QLBillingDataFilter> filtersWithUnallocatedFilter) {
    filtersWithUnallocatedFilter.add(
        QLBillingDataFilter.builder()
            .instanceType(
                QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"CLUSTER_UNALLOCATED"}).build())
            .build());
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    QLSunburstChartData data = (QLSunburstChartData) qlData;
    ValueComparator valueComparator = new ValueComparator();
    List<QLSunburstChartDataPoint> limitProcessedDataPoints = new ArrayList<>();
    PriorityQueue<QLSunburstChartDataPoint> pqParentLevelData = new PriorityQueue<>(valueComparator);
    for (QLSunburstChartDataPoint dataPoint : data.getData()) {
      if (dataPoint.getId().equals(ROOT_PARENT_ID)) {
        limitProcessedDataPoints.add(dataPoint);
      } else {
        pqParentLevelData.add(dataPoint);
      }
    }

    processDataPoints(pqParentLevelData, limitProcessedDataPoints, limit);
    return QLSunburstChartData.builder().data(limitProcessedDataPoints).gridData(data.getGridData()).build();
  }

  List<String> processDataPoints(PriorityQueue<QLSunburstChartDataPoint> pqParentLevelData,
      List<QLSunburstChartDataPoint> limitProcessedDataPoints, Integer limit) {
    List<String> retainedElements = new ArrayList<>();
    QLSunburstChartDataPointBuilder othersDataPoint = QLSunburstChartDataPoint.builder();
    Double othersDataPointValue = 0.0;
    boolean processOthersDataPoint = true;
    int parentCounter = 0;
    while (!pqParentLevelData.isEmpty()) {
      if (parentCounter++ < limit) {
        QLSunburstChartDataPoint dataPoint = pqParentLevelData.poll();
        limitProcessedDataPoints.add(dataPoint);
        retainedElements.add(dataPoint.getId());
      } else {
        QLSunburstChartDataPoint dataPoint = pqParentLevelData.poll();
        if (processOthersDataPoint) {
          String otherDataPointParent = dataPoint.getParent();
          othersDataPoint.parent(otherDataPointParent);
          othersDataPoint.id(otherDataPointParent + "_" + OTHERS);
          othersDataPoint.name(OTHERS);
          othersDataPoint.type(dataPoint.getType());
          othersDataPoint.clusterType(dataPoint.getClusterType());
          othersDataPoint.instanceType(dataPoint.getInstanceType());
          processOthersDataPoint = false;
        }
        othersDataPointValue += dataPoint.getValue().doubleValue();
      }
    }
    if (!processOthersDataPoint) {
      limitProcessedDataPoints.add(othersDataPoint.value(othersDataPointValue).build());
    }
    return retainedElements;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
