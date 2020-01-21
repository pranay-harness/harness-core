package software.wings.graphql.datafetcher.billing;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class SunburstChartStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject QLBillingStatsHelper billingStatsHelper;
  @Inject BillingDataHelper billingDataHelper;

  private static final String TOTAL_COST_VALUE = "$%s";
  private static final String INFO = "Idle Cost %s";
  private static final String ROOT_PARENT_ID = "ROOT_PARENT_ID";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    try {
      if (timeScaleDBService.isValid()) {
        if (groupBy.size() > 2) {
          List<QLCCMGroupBy> k8sGroupBy = new ArrayList<>(groupBy);
          List<QLCCMGroupBy> ecsGroupBy = new ArrayList<>(groupBy);
          processAndRetainK8sGroupBy(k8sGroupBy);
          processAndRetainEcsGroupBy(ecsGroupBy);
          List<QLSunburstChartDataPoint> k8sChartData =
              getSunburstChartData(accountId, aggregateFunction, filters, k8sGroupBy, sort, true);
          List<QLSunburstChartDataPoint> ecsChartData =
              getSunburstChartData(accountId, aggregateFunction, filters, ecsGroupBy, sort, false);
          List<QLSunburstChartDataPoint> chartData = k8sChartData;
          chartData.addAll(ecsChartData);
          List<QLSunburstGridDataPoint> gridData =
              getSunburstGridData(accountId, aggregateFunction, filters, groupBy, sort);

          return QLSunburstChartData.builder().data(chartData).gridData(gridData).build();
        }

        List<QLSunburstChartDataPoint> sunburstChartDataPointList =
            getSunburstChartData(accountId, aggregateFunction, filters, groupBy, sort, true);

        List<QLSunburstGridDataPoint> sunburstGridDataPointList =
            getSunburstGridData(accountId, aggregateFunction, filters, groupBy, sort);

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

  @VisibleForTesting
  List<QLSunburstGridDataPoint> getSunburstGridData(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupBy);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupBy);

    queryData = billingDataQueryBuilder.formQuery(accountId, filters, aggregateFunction,
        groupByEntityList.isEmpty() ? Collections.emptyList() : Collections.singletonList(groupByEntityList.get(0)),
        groupByTime, sort, true);
    logger.info("SunburstGridStatsDataFetcher query: {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateSunburstGridData(queryData, resultSet);
    } catch (SQLException e) {
      logger.error("BillingStatsTimeSeriesDataFetcher Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return Collections.emptyList();
  }

  private List<QLSunburstGridDataPoint> generateSunburstGridData(
      BillingDataQueryMetadata queryData, ResultSet resultSet) throws SQLException {
    List<QLSunburstGridDataPoint> sunburstGridDataPointList = new ArrayList<>();
    while (null != resultSet && resultSet.next()) {
      QLSunburstGridDataPointBuilder gridDataPointBuilder = QLSunburstGridDataPoint.builder();
      for (BillingDataQueryMetadata.BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            switch (field) {
              case SUM:
                gridDataPointBuilder.value(
                    String.format(TOTAL_COST_VALUE, billingDataHelper.roundingDoubleFieldValue(field, resultSet)));
                break;
              case IDLECOST:
                gridDataPointBuilder.info(
                    String.format(INFO, billingDataHelper.roundingDoubleFieldValue(field, resultSet)) + "%");
                break;
              default:
                throw new InvalidRequestException("UnsupportedType " + field.getDataType());
            }
            break;
          case STRING:
            gridDataPointBuilder.name(
                billingStatsHelper.getEntityName(field, resultSet.getString(field.getFieldName())));
            break;
          default:
            throw new InvalidRequestException("UnsupportedType " + field.getDataType());
        }
      }
      sunburstGridDataPointList.add(gridDataPointBuilder.build());
    }
    return sunburstGridDataPointList;
  }

  @VisibleForTesting
  List<QLSunburstChartDataPoint> getSunburstChartData(String accountId,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy,
      List<QLBillingSortCriteria> sort, boolean addRootParent) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupBy);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupBy);
    queryData = billingDataQueryBuilder.formQuery(
        accountId, filters, aggregateFunction, groupByEntityList, groupByTime, sort, false);
    logger.info("SunburstChartStatsDataFetcher query: {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());

      return generateSunburstChartData(queryData, resultSet, addRootParent);
    } catch (SQLException e) {
      logger.error("BillingStatsTimeSeriesDataFetcher Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return Collections.emptyList();
  }

  private List<QLSunburstChartDataPoint> generateSunburstChartData(
      BillingDataQueryMetadata queryData, ResultSet resultSet, boolean addRootParent) throws SQLException {
    List<BillingDataQueryMetadata.BillingDataMetaDataFields> groupByFields = queryData.getGroupByFields();
    BillingDataQueryMetadata.BillingDataMetaDataFields parentField = null;
    BillingDataQueryMetadata.BillingDataMetaDataFields childField = groupByFields.get(0);
    if (groupByFields.size() > 1) {
      parentField = groupByFields.get(0);
      childField = groupByFields.get(1);
    }

    Set<String> parentIdSet = new HashSet<>();
    List<QLSunburstChartDataPoint> sunburstChartDataPoints = new ArrayList<>();
    addRootParentIdIfSpecified(addRootParent, sunburstChartDataPoints);

    while (resultSet != null && resultSet.next()) {
      QLSunburstChartDataPointBuilder dataPointBuilder = QLSunburstChartDataPoint.builder();

      // ParentId
      if (parentField == null) {
        dataPointBuilder.parent(ROOT_PARENT_ID);
      } else {
        setParentIdAndAddParentDataPoint(
            resultSet, parentField, parentIdSet, dataPointBuilder, sunburstChartDataPoints);
      }

      // Id and Name
      String id = resultSet.getString(childField.getFieldName());
      dataPointBuilder.id(id);
      dataPointBuilder.name(billingStatsHelper.getEntityName(childField, id));
      // Value
      dataPointBuilder.value(
          resultSet.getBigDecimal(BillingDataQueryMetadata.BillingDataMetaDataFields.SUM.getFieldName()));
      sunburstChartDataPoints.add(dataPointBuilder.build());
    }
    return sunburstChartDataPoints;
  }

  private void setParentIdAndAddParentDataPoint(ResultSet resultSet,
      BillingDataQueryMetadata.BillingDataMetaDataFields parentField, Set<String> parentIdSet,
      QLSunburstChartDataPointBuilder dataPointBuilder, List<QLSunburstChartDataPoint> sunburstChartDataPoints)
      throws SQLException {
    String parentId = resultSet.getString(parentField.getFieldName());
    dataPointBuilder.parent(parentId);
    if (!parentIdSet.contains(parentId)) {
      String id = resultSet.getString(parentField.getFieldName());
      boolean setClusterType = false;
      String clusterType = null;
      if (parentField.getFieldName().equals(
              BillingDataQueryMetadata.BillingDataMetaDataFields.CLUSTERID.getFieldName())) {
        clusterType =
            resultSet.getString(BillingDataQueryMetadata.BillingDataMetaDataFields.CLUSTERTYPE.getFieldName());
        setClusterType = true;
      }
      sunburstChartDataPoints.add(QLSunburstChartDataPoint.builder()
                                      .parent(ROOT_PARENT_ID)
                                      .id(id)
                                      .name(billingStatsHelper.getEntityName(parentField, id))
                                      .clusterType(setClusterType ? clusterType : null)
                                      .build());
      parentIdSet.add(parentId);
    }
  }

  private void addRootParentIdIfSpecified(
      boolean addRootParent, List<QLSunburstChartDataPoint> sunburstChartDataPoints) {
    final String ROOT_PARENT_CONSTANT = "";
    if (addRootParent) {
      sunburstChartDataPoints.add(QLSunburstChartDataPoint.builder()
                                      .id(ROOT_PARENT_ID)
                                      .name(ROOT_PARENT_CONSTANT)
                                      .parent(ROOT_PARENT_CONSTANT)
                                      .build());
    }
  }

  private void processAndRetainK8sGroupBy(List<QLCCMGroupBy> groupBy) {
    groupBy.removeIf(groupByItem -> groupByItem.getEntityGroupBy() == QLCCMEntityGroupBy.CloudServiceName);
  }

  private void processAndRetainEcsGroupBy(List<QLCCMGroupBy> groupBy) {
    groupBy.removeIf(groupByItem -> groupByItem.getEntityGroupBy() == QLCCMEntityGroupBy.Namespace);
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
