package software.wings.graphql.datafetcher.billing;

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
import software.wings.graphql.schema.type.aggregation.billing.QLContextInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLEfficiencyScoreInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLEfficiencyStatsData;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo.QLStatsBreakdownInfoBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

@Slf4j
public class EfficiencyStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataHelper billingDataHelper;

  private static final String TOTAL_COST_DESCRIPTION = "Total Cost between %s - %s";
  private static int idleCostBaseline = 30;
  private static int unallocatedCostBaseline = 5;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, aggregateFunction, filters, groupBy, sort);
      } else {
        throw new InvalidRequestException("Cannot process request");
      }
    } catch (Exception e) {
      throw new InvalidRequestException(
          "Error while connecting to the TimeScale DB in EfficiencyStats Data Fetcher", e);
    }
  }

  protected QLEfficiencyStatsData getData(@NotNull String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    QLStatsBreakdownInfo costStats = QLStatsBreakdownInfo.builder().build();
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupBy);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupBy);

    queryData = billingDataQueryBuilder.formQuery(accountId, filters, aggregateFunction,
        groupByEntityList.isEmpty() ? Collections.emptyList() : groupByEntityList, groupByTime, sort, true, true);
    logger.info("getSunburstGridData query: {}", queryData.getQuery());

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            accountId, aggregateFunction, filters, groupByEntityList, groupByTime, sort);

    StringJoiner entityIdAppender = new StringJoiner(":");
    QLBillingAmountData prevBillingAmountData = entityIdToPrevBillingAmountData.get(entityIdAppender.toString());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      costStats =
          getCostBreakdown(queryData, resultSet, entityIdToPrevBillingAmountData, filters, entityIdAppender.toString());
    } catch (SQLException e) {
      logger.error("SunburstChartStatsDataFetcher (getSunburstGridData) Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
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
    int efficiencyScore = calculateEfficiencyScore(costStats);
    QLStatsBreakdownInfo prevCostStats = QLStatsBreakdownInfo.builder()
                                             .total(prevBillingAmountData.getCost())
                                             .idle(prevBillingAmountData.getIdleCost())
                                             .unallocated(prevBillingAmountData.getUnallocatedCost())
                                             .build();
    double prevUtilizedCost = billingDataHelper.getRoundedDoubleValue(prevCostStats.getTotal().doubleValue()
        - prevCostStats.getIdle().doubleValue() - prevCostStats.getUnallocated().doubleValue());
    prevCostStats.setUtilized(prevUtilizedCost);
    int oldEfficiencyScore = calculateEfficiencyScore(prevCostStats);

    BigDecimal effScoreDiff = BigDecimal.valueOf((long) efficiencyScore - (long) oldEfficiencyScore);

    BigDecimal trendPercentage = effScoreDiff.multiply(BigDecimal.valueOf(100))
                                     .divide(BigDecimal.valueOf(oldEfficiencyScore), 2, RoundingMode.HALF_UP);
    return QLEfficiencyScoreInfo.builder()
        .efficiencyScore(efficiencyScore)
        .trend(billingDataHelper.getRoundedDoubleValue(trendPercentage))
        .build();
  }

  private int calculateEfficiencyScore(QLStatsBreakdownInfo costStats) {
    int utilizedBaseline = 100 - idleCostBaseline - unallocatedCostBaseline;
    double utilized = costStats.getUtilized().doubleValue();
    double total = costStats.getTotal().doubleValue();
    double utilizedPercentage = utilized / total * 100;
    int efficiencyScore = (int) ((1 - ((utilizedBaseline - utilizedPercentage) / utilizedBaseline)) * 100);
    return efficiencyScore > 100 ? 100 : efficiencyScore;
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
}
