package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingTrendStats;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingTrendStatsDataFetcher extends AbstractStatsDataFetcher<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject BillingDataHelper billingDataHelper;

  private static final long ONE_DAY_MILLIS = 86400000;
  private static final String TOTAL_COST_DESCRIPTION = "of %s - %s";
  private static final String TOTAL_COST_LABEL = "Total Cost";
  private static final String TREND_COST_LABEL = "Cost Trend";
  private static final String FORECAST_COST_LABEL = "Forecasted total cost";
  private static final String TREND_COST_DESCRIPTION = "$%s over %s - %s";
  private static final String FORECAST_COST_DESCRIPTION = "of %s - %s";
  private static final String TOTAL_COST_VALUE = "$%s";
  private static final String TREND_COST_VALUE = "%s";
  private static final String FORECAST_COST_VALUE = "$%s";
  private static final String EMPTY_VALUE = "-";
  private static final String NA_VALUE = "NA";

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, QLCCMAggregationFunction aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, aggregateFunction, filters);
      } else {
        throw new InvalidRequestException("Cannot process request");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while billing data", e);
    }
  }

  protected QLBillingTrendStats getData(
      @NotNull String accountId, QLCCMAggregationFunction aggregateFunction, List<QLBillingDataFilter> filters) {
    QLBillingAmountData billingAmountData = getBillingAmountData(accountId, aggregateFunction, filters);
    if (billingAmountData != null) {
      BigDecimal totalBillingAmount = billingAmountData.getCost();
      BigDecimal forecastCost =
          billingDataHelper.getForecastCost(billingAmountData, billingDataHelper.getEndInstant(filters));
      return QLBillingTrendStats.builder()
          .totalCost(getTotalBillingStats(billingAmountData, filters))
          .costTrend(getBillingTrend(accountId, totalBillingAmount, forecastCost, aggregateFunction, filters))
          .forecastCost(getForecastBillingStats(forecastCost, billingAmountData,
              billingDataHelper.getStartInstant(filters), billingDataHelper.getEndInstant(filters)))
          .build();
    } else {
      return QLBillingTrendStats.builder().build();
    }
  }

  private QLBillingStatsInfo getForecastBillingStats(
      BigDecimal forecastCost, QLBillingAmountData billingAmountData, Instant startInstant, Instant endInstant) {
    String forecastCostDescription = EMPTY_VALUE;
    String forecastCostValue = EMPTY_VALUE;
    long timeDiff = billingAmountData.getMaxStartTime() - startInstant.toEpochMilli();
    if (forecastCost != null && timeDiff > 4 * ONE_DAY_MILLIS) {
      String startInstantFormat = billingDataHelper.getTotalCostFormattedDate(startInstant);
      String endInstantFormat = billingDataHelper.getTotalCostFormattedDate(endInstant);
      forecastCostDescription = String.format(FORECAST_COST_DESCRIPTION, startInstantFormat, endInstantFormat);
      forecastCostValue = String.format(FORECAST_COST_VALUE, getRoundedDoubleValue(forecastCost));
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(FORECAST_COST_LABEL)
        .statsDescription(forecastCostDescription)
        .statsValue(forecastCostValue)
        .build();
  }

  private QLBillingStatsInfo getTotalBillingStats(
      QLBillingAmountData billingAmountData, List<QLBillingDataFilter> filters) {
    Instant startInstant = Instant.ofEpochMilli(billingDataHelper.getStartTimeFilter(filters).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(billingAmountData.getMaxStartTime());
    String startInstantFormat = billingDataHelper.getTotalCostFormattedDate(startInstant);
    String endInstantFormat = billingDataHelper.getTotalCostFormattedDate(endInstant);
    String totalCostDescription = String.format(TOTAL_COST_DESCRIPTION, startInstantFormat, endInstantFormat);
    String totalCostValue = String.format(TOTAL_COST_VALUE, getRoundedDoubleValue(billingAmountData.getCost()));
    return QLBillingStatsInfo.builder()
        .statsLabel(TOTAL_COST_LABEL)
        .statsDescription(totalCostDescription)
        .statsValue(totalCostValue)
        .build();
  }

  private QLBillingStatsInfo getBillingTrend(String accountId, BigDecimal totalBillingAmount, BigDecimal forecastCost,
      QLCCMAggregationFunction aggregateFunction, List<QLBillingDataFilter> filters) {
    List<QLBillingDataFilter> trendFilters = billingDataHelper.getTrendFilter(
        filters, billingDataHelper.getStartInstant(filters), billingDataHelper.getEndInstant(filters));
    QLBillingAmountData prevBillingAmountData = getBillingAmountData(accountId, aggregateFunction, trendFilters);
    Instant filterStartTime =
        Instant.ofEpochMilli(billingDataHelper.getStartTimeFilter(trendFilters).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(billingDataHelper.getEndTimeFilter(trendFilters).getValue().longValue());
    String trendCostDescription = String.format(TREND_COST_DESCRIPTION, getRoundedDoubleValue(totalBillingAmount),
        billingDataHelper.getTotalCostFormattedDate(filterStartTime),
        billingDataHelper.getTotalCostFormattedDate(endInstant));
    String trendCostValue = NA_VALUE;
    if (prevBillingAmountData != null && prevBillingAmountData.getCost().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal prevTotalBillingAmount = prevBillingAmountData.getCost();
      Instant startInstant = Instant.ofEpochMilli(prevBillingAmountData.getMinStartTime());
      String startInstantFormat = billingDataHelper.getTotalCostFormattedDate(startInstant);
      String endInstantFormat = billingDataHelper.getTotalCostFormattedDate(endInstant);
      BigDecimal amountDifference = totalBillingAmount.subtract(prevTotalBillingAmount);
      if (null != forecastCost) {
        amountDifference = forecastCost.subtract(prevTotalBillingAmount);
      }
      trendCostDescription = String.format(TREND_COST_DESCRIPTION, Math.abs(getRoundedDoubleValue(amountDifference)),
          startInstantFormat, endInstantFormat);
      if (amountDifference.compareTo(BigDecimal.ZERO) < 0) {
        trendCostDescription = EMPTY_VALUE.concat(trendCostDescription);
      }
      if (filterStartTime.plus(1, ChronoUnit.DAYS).isAfter(startInstant)) {
        BigDecimal trendPercentage =
            amountDifference.multiply(BigDecimal.valueOf(100)).divide(prevTotalBillingAmount, 2, RoundingMode.HALF_UP);
        trendCostValue = String.format(TREND_COST_VALUE, getRoundedDoubleValue(trendPercentage));
      }
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(TREND_COST_LABEL)
        .statsDescription(trendCostDescription)
        .statsValue(trendCostValue)
        .build();
  }

  public QLBillingAmountData getBillingAmountData(
      String accountId, QLCCMAggregationFunction aggregateFunction, List<QLBillingDataFilter> filters) {
    BillingDataQueryMetadata queryData =
        billingDataQueryBuilder.formTrendStatsQuery(accountId, aggregateFunction, filters);
    String query = queryData.getQuery();
    logger.info("Billing data query {}", query);
    ResultSet resultSet = null;
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      return fetchBillingAmount(resultSet);
    } catch (SQLException e) {
      logger.error("BillingStatsTimeSeriesDataFetcher Error exception", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private QLBillingAmountData fetchBillingAmount(ResultSet resultSet) throws SQLException {
    while (null != resultSet && resultSet.next()) {
      if (resultSet.getBigDecimal(BillingDataMetaDataFields.SUM.getFieldName()) != null) {
        return QLBillingAmountData.builder()
            .cost(resultSet.getBigDecimal(BillingDataMetaDataFields.SUM.getFieldName()))
            .minStartTime(
                resultSet
                    .getTimestamp(BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(), utils.getDefaultCalendar())
                    .getTime())
            .maxStartTime(
                resultSet
                    .getTimestamp(BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(), utils.getDefaultCalendar())
                    .getTime())
            .build();
      }
    }
    return null;
  }

  private double getRoundedDoubleValue(BigDecimal value) {
    return Math.round(value.doubleValue() * 100D) / 100D;
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
