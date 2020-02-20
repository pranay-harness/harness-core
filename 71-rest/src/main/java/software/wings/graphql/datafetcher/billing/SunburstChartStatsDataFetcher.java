package software.wings.graphql.datafetcher.billing;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.Account;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

@Slf4j
public class SunburstChartStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject QLBillingStatsHelper billingStatsHelper;
  @Inject BillingDataHelper billingDataHelper;

  private static final String ROOT_PARENT_ID = "ROOT_PARENT_ID";
  private static int idleCostBaseline = 30;
  private static int unallocatedCostBaseline = 5;
  private static String unsupportedType = "UnsupportedType ";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    try {
      if (timeScaleDBService.isValid()) {
        if (groupBy.size() > 4) {
          List<QLCCMGroupBy> k8sGroupBy = new ArrayList<>(groupBy);
          List<QLCCMGroupBy> ecsGroupBy = new ArrayList<>(groupBy);
          processAndRetainK8sGroupBy(k8sGroupBy);
          processAndRetainEcsGroupBy(ecsGroupBy);
          List<QLSunburstGridDataPoint> k8sGridData =
              getGridDataPointsForAllViews(accountId, aggregateFunction, filters, k8sGroupBy, sort, true);
          List<QLSunburstGridDataPoint> ecsGridData =
              getGridDataPointsForAllViews(accountId, aggregateFunction, filters, ecsGroupBy, sort, false);
          List<QLSunburstGridDataPoint> gridData = new ArrayList<>(k8sGridData);
          gridData.addAll(ecsGridData);
          List<QLSunburstChartDataPoint> k8sChartData = getSunburstChartData(
              accountId, aggregateFunction, filters, k8sGroupBy, sort, true, convertGridPointsToMap(gridData));
          List<QLSunburstChartDataPoint> ecsChartData = getSunburstChartData(
              accountId, aggregateFunction, filters, ecsGroupBy, sort, false, convertGridPointsToMap(gridData));
          List<QLSunburstChartDataPoint> chartData = k8sChartData;
          chartData.addAll(ecsChartData);
          return QLSunburstChartData.builder().data(chartData).gridData(gridData).build();
        }

        List<QLSunburstGridDataPoint> sunburstGridDataPointList =
            getGridDataPointsForAllViews(accountId, aggregateFunction, filters, groupBy, sort, true);
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
      List<QLBillingSortCriteria> sort, boolean computeClusterData) {
    List<QLSunburstGridDataPoint> sunburstGridDataPointList = new ArrayList<>();
    List<QLCCMGroupBy> modifiedGroupBy = new ArrayList<>();
    for (QLCCMGroupBy groupByPoint : groupBy) {
      boolean isClusterGroupBy = false;
      if (groupByPoint.getEntityGroupBy() == QLCCMEntityGroupBy.Cluster) {
        isClusterGroupBy = true;
        if (!computeClusterData) {
          modifiedGroupBy.add(groupByPoint);
          continue;
        }
      }
      if (groupByPoint.getEntityGroupBy() != QLCCMEntityGroupBy.ClusterType
          && groupByPoint.getEntityGroupBy() != QLCCMEntityGroupBy.InstanceType) {
        modifiedGroupBy.add(groupByPoint);
        sunburstGridDataPointList.addAll(getSunburstGridData(
            accountId, aggregateFunction, new ArrayList<>(filters), modifiedGroupBy, sort, isClusterGroupBy));
      }
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
        groupByEntityList.isEmpty() ? Collections.emptyList() : groupByEntityList, groupByTime, sort, isClusterGroupBy);
    logger.info("getSunburstGridData query: {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateSunburstGridData(
          queryData, resultSet, unallocatedCostMap, isClusterGroupBy, groupByEntityList.size());
    } catch (SQLException e) {
      logger.error("SunburstChartStatsDataFetcher (getSunburstGridData) Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return Collections.emptyList();
  }

  private List<QLSunburstGridDataPoint> generateSunburstGridData(BillingDataQueryMetadata queryData,
      ResultSet resultSet, Map<String, Double> unallocatedCostMap, boolean isClusterGroupBy, int groupBySize)
      throws SQLException {
    List<QLSunburstGridDataPoint> sunburstGridDataPointList = new ArrayList<>();
    while (null != resultSet && resultSet.next()) {
      QLSunburstGridDataPointBuilder gridDataPointBuilder = QLSunburstGridDataPoint.builder();
      QLStatsBreakdownInfoBuilder qlStatsBreakdownInfoBuilder = QLStatsBreakdownInfo.builder();
      StringJoiner entityIdAppender = new StringJoiner(":");
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
                if (groupBySize == 1) {
                  qlStatsBreakdownInfoBuilder.unallocated(
                      unallocatedCostMap.get(entityId) != null ? unallocatedCostMap.get(entityId) : 0);
                  entityIdAppender.add(entityId);
                  gridDataPointBuilder.id(entityIdAppender.toString());
                  gridDataPointBuilder.type(fieldName);
                  gridDataPointBuilder.name(billingStatsHelper.getEntityName(field, entityId));
                }
                entityIdAppender.add(entityId);
                break;
              case NAMESPACE:
              case CLOUDSERVICENAME:
              case ENVID:
                fieldName = field.getFieldName();
                entityId = resultSet.getString(fieldName);
                if (groupBySize == 2) {
                  qlStatsBreakdownInfoBuilder.unallocated(0);
                  entityIdAppender.add(entityId);
                  gridDataPointBuilder.id(entityIdAppender.toString());
                  gridDataPointBuilder.type(fieldName);
                  gridDataPointBuilder.name(billingStatsHelper.getEntityName(field, entityId));
                }
                entityIdAppender.add(entityId);
                break;
              case WORKLOADNAME:
              case TASKID:
              case SERVICEID:
                fieldName = field.getFieldName();
                entityId = resultSet.getString(fieldName);
                if (groupBySize == 3) {
                  qlStatsBreakdownInfoBuilder.unallocated(0);
                  entityIdAppender.add(entityId);
                  gridDataPointBuilder.id(entityIdAppender.toString());
                  gridDataPointBuilder.type(fieldName);
                  gridDataPointBuilder.name(billingStatsHelper.getEntityName(field, entityId));
                }
                break;
              default:
                throw new InvalidRequestException(unsupportedType + field.getDataType());
            }
            break;
          default:
            throw new InvalidRequestException(unsupportedType + field.getDataType());
        }
      }
      QLStatsBreakdownInfo breakdownInfo = qlStatsBreakdownInfoBuilder.build();
      validateBreakdownInfo(breakdownInfo, isClusterGroupBy);
      gridDataPointBuilder.efficiencyScore(calculateEfficiencyScore(breakdownInfo, isClusterGroupBy));
      gridDataPointBuilder.value(breakdownInfo.getTotal());
      // TODO: (ROHIT) Fix the Trend (Added this for Demo Reasons)
      gridDataPointBuilder.trend(Math.round(Math.random() * 10 * 100D) / 100D);
      sunburstGridDataPointList.add(gridDataPointBuilder.build());
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
    BillingDataQueryMetadata.BillingDataMetaDataFields childField = groupByFields.get(1);
    BillingDataQueryMetadata.BillingDataMetaDataFields leafField = groupByFields.get(2);

    List<QLSunburstChartDataPoint> sunburstChartDataPoints = new ArrayList<>();
    addRootParentIdIfSpecified(addRootParent, sunburstChartDataPoints, contextName);
    // First in the pair is the first level Id's and so on ..
    Set<Pair<String, String>> pairOfNonLeafEntities = new HashSet<>();
    Set<Pair<String, String>> parentIdAndClusterTypeSet = new HashSet<>();
    Map<String, Double> childIdCostMap = new HashMap<>();
    Map<String, String> instanceTypeMap = new HashMap<>();

    while (resultSet != null && resultSet.next()) {
      QLSunburstChartDataPointBuilder dataPointBuilder = QLSunburstChartDataPoint.builder();
      String parentFieldName = resultSet.getString(parentField.getFieldName());
      String chileFieldName = resultSet.getString(childField.getFieldName());
      // Add Inner two fields data into a Set for adding Data Points
      String instanceType = getInstanceType(resultSet);
      pairOfNonLeafEntities.add(Pair.of(parentFieldName, chileFieldName));
      String clusterType = checkAndSetClusterType(resultSet, parentField);
      parentIdAndClusterTypeSet.add(Pair.of(parentFieldName, clusterType));

      String id = resultSet.getString(leafField.getFieldName());
      dataPointBuilder.name(billingStatsHelper.getEntityName(leafField, id));
      dataPointBuilder.type(leafField.getFieldName());

      String parentId = parentFieldName + ":" + chileFieldName;
      dataPointBuilder.parent(parentFieldName + ":" + chileFieldName);
      String uniqueId = parentId + ":" + id;
      dataPointBuilder.id(uniqueId);
      dataPointBuilder.instanceType(instanceType);
      dataPointBuilder.metadata(sunburstGridDataPointMap.get(uniqueId));
      double value =
          resultSet.getBigDecimal(BillingDataQueryMetadata.BillingDataMetaDataFields.SUM.getFieldName()).doubleValue();
      dataPointBuilder.value(billingDataHelper.getRoundedDoubleValue(value));
      childIdCostMap.put(parentId, childIdCostMap.getOrDefault(parentId, 0.0) + value);
      instanceTypeMap.putIfAbsent(parentId, instanceType);
      sunburstChartDataPoints.add(dataPointBuilder.build());
    }

    // Add Children Data Points
    for (Pair<String, String> pairOfIds : pairOfNonLeafEntities) {
      String parentId = pairOfIds.getKey();
      String childId = pairOfIds.getValue();
      QLSunburstChartDataPointBuilder dataPointBuilder = QLSunburstChartDataPoint.builder();
      String id = parentId + ":" + childId;
      dataPointBuilder.id(id);
      dataPointBuilder.name(billingStatsHelper.getEntityName(childField, childId));
      dataPointBuilder.type(childField.getFieldName());
      dataPointBuilder.metadata(sunburstGridDataPointMap.get(id));
      dataPointBuilder.value(
          childIdCostMap.get(id) != null ? billingDataHelper.getRoundedDoubleValue(childIdCostMap.get(id)) : null);
      dataPointBuilder.instanceType(instanceTypeMap.get(id));
      dataPointBuilder.parent(parentId);
      sunburstChartDataPoints.add(dataPointBuilder.build());
    }

    // Add First Level Data Points (Parent)
    for (Pair<String, String> parentIds : parentIdAndClusterTypeSet) {
      String id = parentIds.getKey();
      String clusterType = parentIds.getValue();
      QLSunburstChartDataPointBuilder dataPointBuilder = QLSunburstChartDataPoint.builder();
      dataPointBuilder.id(id);
      dataPointBuilder.metadata(sunburstGridDataPointMap.get(id));
      dataPointBuilder.name(billingStatsHelper.getEntityName(parentField, id));
      dataPointBuilder.type(parentField.getFieldName());
      dataPointBuilder.parent(ROOT_PARENT_ID);
      dataPointBuilder.clusterType(clusterType);
      sunburstChartDataPoints.add(dataPointBuilder.build());
    }

    return sunburstChartDataPoints;
  }

  private String getInstanceType(ResultSet resultSet) throws SQLException {
    return resultSet.getString(BillingDataQueryMetadata.BillingDataMetaDataFields.INSTANCETYPE.getFieldName());
  }

  private String checkAndSetClusterType(
      ResultSet resultSet, BillingDataQueryMetadata.BillingDataMetaDataFields parentField) throws SQLException {
    if (parentField == BillingDataQueryMetadata.BillingDataMetaDataFields.CLUSTERID) {
      return resultSet.getString(BillingDataQueryMetadata.BillingDataMetaDataFields.CLUSTERTYPE.getFieldName());
    }
    return null;
  }

  private void addRootParentIdIfSpecified(
      boolean addRootParent, List<QLSunburstChartDataPoint> sunburstChartDataPoints, String contextName) {
    final String ROOT_PARENT_CONSTANT = "";
    if (addRootParent) {
      sunburstChartDataPoints.add(
          QLSunburstChartDataPoint.builder().id(ROOT_PARENT_ID).name(contextName).parent(ROOT_PARENT_CONSTANT).build());
    }
  }

  private void processAndRetainK8sGroupBy(List<QLCCMGroupBy> groupBy) {
    groupBy.removeIf(groupByItem
        -> groupByItem.getEntityGroupBy() == QLCCMEntityGroupBy.CloudServiceName
            || groupByItem.getEntityGroupBy() == QLCCMEntityGroupBy.TaskId);
  }

  private void processAndRetainEcsGroupBy(List<QLCCMGroupBy> groupBy) {
    groupBy.removeIf(groupByItem
        -> groupByItem.getEntityGroupBy() == QLCCMEntityGroupBy.Namespace
            || groupByItem.getEntityGroupBy() == QLCCMEntityGroupBy.WorkloadName);
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
      List<QLCCMAggregationFunction> aggregateFunction, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
