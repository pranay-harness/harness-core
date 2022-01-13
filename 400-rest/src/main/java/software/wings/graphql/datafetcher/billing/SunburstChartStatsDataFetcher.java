/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo.QLStatsBreakdownInfoBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstChartData;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstGridDataPoint;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstGridDataPoint.QLSunburstGridDataPointBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import graphql.schema.DataFetchingEnvironment;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class SunburstChartStatsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<QLCCMAggregationFunction, QLBillingDataFilter,
        QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject QLBillingStatsHelper billingStatsHelper;
  @Inject BillingDataHelper billingDataHelper;
  @Inject CeAccountExpirationChecker accountChecker;

  private static int idleCostBaseline = 30;
  private static int unallocatedCostBaseline = 5;
  private static String unsupportedType = "UnsupportedType ";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort, Integer limit,
      Integer offset) {
    accountChecker.checkIsCeEnabled(accountId);
    try {
      if (timeScaleDBService.isValid()) {
        boolean isClusterGroupBy = false;
        if (!groupBy.isEmpty()) {
          isClusterGroupBy = groupBy.get(0).getEntityGroupBy() == QLCCMEntityGroupBy.Cluster;
        }
        return getSunburstGridData(accountId, aggregateFunction, filters, groupBy, sort, isClusterGroupBy);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingStatsFilterValuesDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  @VisibleForTesting
  QLSunburstChartData getSunburstGridData(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort,
      boolean isClusterGroupBy) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    int retryCount = 0;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupBy);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupBy);

    queryData = billingDataQueryBuilder.formQuery(accountId, filters, aggregateFunction,
        groupByEntityList.isEmpty() ? Collections.emptyList() : groupByEntityList, groupByTime, sort, isClusterGroupBy,
        true);
    log.info("getSunburstGridData query: {}", queryData.getQuery());
    List<QLCCMEntityGroupBy> modifiedEntityGroupBy = filterOutClusterTypeGroupBy(groupByEntityList);
    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            accountId, aggregateFunction, filters, modifiedEntityGroupBy, groupByTime, sort);

    while (retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        return generateSunburstGridData(
            queryData, resultSet, isClusterGroupBy, entityIdToPrevBillingAmountData, filters);
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          log.error(
              "Failed to execute query in SunburstChartStatsDataFetcher, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          log.warn(
              "Failed to execute query in SunburstChartStatsDataFetcher, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return QLSunburstChartData.builder().build();
  }

  private QLSunburstChartData generateSunburstGridData(BillingDataQueryMetadata queryData, ResultSet resultSet,
      boolean isClusterGroupBy, Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData,
      List<QLBillingDataFilter> filters) throws SQLException {
    Double totalCost = Double.valueOf(0);
    List<QLSunburstGridDataPoint> sunburstGridDataPointList = new ArrayList<>();
    Double costTrend = BillingStatsDefaultKeys.COSTTREND;
    while (null != resultSet && resultSet.next()) {
      QLSunburstGridDataPointBuilder gridDataPointBuilder = QLSunburstGridDataPoint.builder();
      QLStatsBreakdownInfoBuilder qlStatsBreakdownInfoBuilder = QLStatsBreakdownInfo.builder();
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            switch (field) {
              case SUM:
                double cost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
                qlStatsBreakdownInfoBuilder.total(cost);
                totalCost += cost;
                break;
              case IDLECOST:
                qlStatsBreakdownInfoBuilder.idle(billingDataHelper.roundingDoubleFieldValue(field, resultSet));
                break;
              case UNALLOCATEDCOST:
                qlStatsBreakdownInfoBuilder.unallocated(billingDataHelper.roundingDoubleFieldValue(field, resultSet));
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
                gridDataPointBuilder.id(entityId);
                gridDataPointBuilder.type(fieldName);
                gridDataPointBuilder.name(billingStatsHelper.getEntityName(field, entityId));
                break;
              case CLUSTERTYPE:
                gridDataPointBuilder.clusterType(resultSet.getString(field.getFieldName()));
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
      validateBreakdownInfo(breakdownInfo);
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
    return QLSunburstChartData.builder()
        .totalCost(billingDataHelper.getRoundedDoubleValue(totalCost))
        .gridData(sunburstGridDataPointList)
        .build();
  }

  private void validateBreakdownInfo(QLStatsBreakdownInfo breakdownInfo) {
    if (breakdownInfo.getIdle() == null) {
      breakdownInfo.setIdle(0.0);
    }

    if (breakdownInfo.getTotal() == null) {
      breakdownInfo.setTotal(0.0);
    }

    if (breakdownInfo.getUnallocated() == null) {
      breakdownInfo.setUnallocated(0.0);
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

  private List<QLCCMEntityGroupBy> filterOutClusterTypeGroupBy(List<QLCCMEntityGroupBy> groupByEntityList) {
    return groupByEntityList != null ? groupByEntityList.stream()
                                           .filter(entityGroupBy -> entityGroupBy != QLCCMEntityGroupBy.ClusterType)
                                           .collect(Collectors.toList())
                                     : Collections.emptyList();
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    QLSunburstChartData data = (QLSunburstChartData) qlData;
    List<QLSunburstGridDataPoint> gridData = data.getGridData();
    int gridSize = gridData.size();
    List<QLSunburstGridDataPoint> topNGridData = gridData.subList(0, Math.min(gridSize, limit));
    if (includeOthers && gridSize > limit) {
      topNGridData.add(getOtherDataPoint(gridData.subList(limit, gridSize), limit, gridSize));
    }
    return QLSunburstChartData.builder().totalCost(data.getTotalCost()).gridData(topNGridData).build();
  }

  @Override
  protected QLData fetchSelectedFields(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort, Integer limit,
      Integer offset, boolean skipRoundOff, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }

  private QLSunburstGridDataPoint getOtherDataPoint(
      List<QLSunburstGridDataPoint> remainingGridData, Integer limit, Integer gridSize) {
    String othersConst = "Others";
    String type = "defaultType";
    Double trend = Double.valueOf(0);
    Double totalCost = Double.valueOf(0);
    Integer efficiencyScore = 0;
    for (int i = 0; i < gridSize - limit; i++) {
      Double cost = remainingGridData.get(i).getValue().doubleValue();
      totalCost += cost;
      trend += remainingGridData.get(i).getTrend().doubleValue() * cost;
      efficiencyScore += (int) (remainingGridData.get(i).getEfficiencyScore() * cost);
      type = remainingGridData.get(i).getType();
    }
    return QLSunburstGridDataPoint.builder()
        .id(othersConst)
        .name(othersConst)
        .type(type)
        .efficiencyScore((int) (efficiencyScore / totalCost))
        .value(billingDataHelper.getRoundedDoubleValue(totalCost))
        .trend(billingDataHelper.getRoundedDoubleValue(trend / totalCost))
        .build();
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  public boolean isCESampleAccountIdAllowed() {
    return true;
  }
}
