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

import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLContextInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLEfficiencyScoreInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLEfficiencyStatsData;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo.QLStatsBreakdownInfoBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class EfficiencyStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataHelper billingDataHelper;
  @Inject CeAccountExpirationChecker accountChecker;

  private static final String TOTAL_COST_DESCRIPTION = "Total Cost between %s - %s";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    accountChecker.checkIsCeEnabled(accountId);
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, aggregateFunction, filters, groupBy, sort);
      } else {
        throw new InvalidRequestException("Error while connecting to the TimeScale DB in EfficiencyStats Data Fetcher");
      }
    } catch (Exception e) {
      log.error("Cannot Process Request in EfficiencyStats Data Fetcher", e);
    }
    return null;
  }

  protected QLEfficiencyStatsData getData(@NotNull String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    QLStatsBreakdownInfo costStats = QLStatsBreakdownInfo.builder().build();
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    boolean successful = false;
    int retryCount = 0;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupBy);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupBy);

    queryData = billingDataQueryBuilder.formQuery(accountId, filters, aggregateFunction,
        groupByEntityList.isEmpty() ? Collections.emptyList() : groupByEntityList, groupByTime, sort, true, true, true);
    log.info("getSunburstGridData query: {}", queryData.getQuery());

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            accountId, aggregateFunction, filters, groupByEntityList, groupByTime, sort, true);

    StringJoiner entityIdAppender = new StringJoiner(":");
    QLBillingAmountData prevBillingAmountData = entityIdToPrevBillingAmountData.get(entityIdAppender.toString());

    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        costStats = getCostBreakdown(
            queryData, resultSet, entityIdToPrevBillingAmountData, filters, entityIdAppender.toString());
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          log.error(
              "Failed to execute query in EfficiencyStatsDataFetcher, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          log.warn("Failed to execute query in EfficiencyStatsDataFetcher, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }

    return QLEfficiencyStatsData.builder()
        .efficiencyBreakdown(costStats)
        .efficiencyData(getEfficiencyData(costStats, prevBillingAmountData))
        .context(getContextInfo(costStats, filters))
        .build();
  }

  private QLStatsBreakdownInfo getCostBreakdown(BillingDataQueryMetadata queryData, ResultSet resultSet,
      Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData, List<QLBillingDataFilter> filters,
      String entityId) throws SQLException {
    QLStatsBreakdownInfo breakdownInfo = QLStatsBreakdownInfo.builder().build();
    while (null != resultSet && resultSet.next()) {
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
              case UNALLOCATEDCOST:
                qlStatsBreakdownInfoBuilder.unallocated(billingDataHelper.roundingDoubleFieldValue(field, resultSet));
                break;
              default:
                throw new InvalidRequestException("unsupported Type" + field.getDataType());
            }
            break;
          default:
            break;
        }
      }

      if (entityIdToPrevBillingAmountData != null && entityIdToPrevBillingAmountData.containsKey(entityId)) {
        qlStatsBreakdownInfoBuilder.trend(
            billingDataHelper.getCostTrendForEntity(resultSet, entityIdToPrevBillingAmountData.get(entityId), filters));
      }
      breakdownInfo = qlStatsBreakdownInfoBuilder.build();
      if (validateBreakDownInfo(breakdownInfo)) {
        double utilizedCost = billingDataHelper.getRoundedDoubleValue(breakdownInfo.getTotal().doubleValue()
            - breakdownInfo.getIdle().doubleValue() - breakdownInfo.getUnallocated().doubleValue());
        breakdownInfo.setUtilized(utilizedCost);
      }
    }
    return breakdownInfo;
  }

  private boolean validateBreakDownInfo(QLStatsBreakdownInfo breakdownInfo) {
    if (breakdownInfo.getTotal() == null || breakdownInfo.getIdle() == null || breakdownInfo.getUnallocated() == null) {
      throw new InvalidRequestException("Invalid Breakdown Costs, Missing Aggregation");
    }
    return true;
  }

  private QLContextInfo getContextInfo(QLStatsBreakdownInfo costStats, List<QLBillingDataFilter> filters) {
    Instant startInstant = Instant.ofEpochMilli(billingDataHelper.getStartTimeFilter(filters).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(billingDataHelper.getEndTimeFilter(filters).getValue().longValue());
    boolean isYearRequired = billingDataHelper.isYearRequired(startInstant, endInstant);
    String startTime = billingDataHelper.getTotalCostFormattedDate(startInstant, isYearRequired);
    String endTime = billingDataHelper.getTotalCostFormattedDate(endInstant, isYearRequired);
    String totalCostDescription = String.format(TOTAL_COST_DESCRIPTION, startTime, endTime);

    return QLContextInfo.builder()
        .totalCost(costStats.getTotal())
        .costTrend(costStats.getTrend())
        .totalCostDescription(totalCostDescription)
        .build();
  }

  private QLEfficiencyScoreInfo getEfficiencyData(
      QLStatsBreakdownInfo costStats, QLBillingAmountData prevBillingAmountData) {
    int efficiencyScore = billingDataHelper.calculateEfficiencyScore(costStats);
    BigDecimal trendPercentage = BigDecimal.ZERO;
    if (prevBillingAmountData != null && prevBillingAmountData.getCost() != null
        && prevBillingAmountData.getIdleCost() != null && prevBillingAmountData.getUnallocatedCost() != null) {
      QLStatsBreakdownInfo prevCostStats = QLStatsBreakdownInfo.builder()
                                               .total(prevBillingAmountData.getCost())
                                               .idle(prevBillingAmountData.getIdleCost())
                                               .unallocated(prevBillingAmountData.getUnallocatedCost())
                                               .build();
      double prevUtilizedCost = billingDataHelper.getRoundedDoubleValue(prevCostStats.getTotal().doubleValue()
          - prevCostStats.getIdle().doubleValue() - prevCostStats.getUnallocated().doubleValue());
      prevCostStats.setUtilized(prevUtilizedCost);
      int oldEfficiencyScore = billingDataHelper.calculateEfficiencyScore(prevCostStats);
      trendPercentage = billingDataHelper.calculateTrendPercentage(
          BigDecimal.valueOf((long) efficiencyScore), BigDecimal.valueOf((long) oldEfficiencyScore));
    }

    return QLEfficiencyScoreInfo.builder()
        .efficiencyScore(efficiencyScore)
        .trend(billingDataHelper.getRoundedDoubleValue(trendPercentage))
        .build();
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregationFunctions, List<QLBillingSortCriteria> sort, QLData qlData) {
    return null;
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
