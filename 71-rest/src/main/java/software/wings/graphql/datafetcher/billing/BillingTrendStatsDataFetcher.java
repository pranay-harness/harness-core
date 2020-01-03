package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import com.hazelcast.util.Preconditions;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingTrendStatsDataFetcher extends AbstractStatsDataFetcher<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;

  private static final String TOTAL_COST_DESCRIPTION = "of %s - %s";
  private static final String TOTAL_COST_LABEL = "Total Cost";
  private static final String TREND_COST_LABEL = "Cost Trend";
  private static final String FORECAST_COST_LABEL = "Forecasted total cost";
  private static final String TREND_COST_DESCRIPTION = "$%s over %s - %s";
  private static final String FORECAST_COST_DESCRIPTION = "of %s - %s";
  private static final String TOTAL_COST_VALUE = "$%s";
  private static final String TREND_COST_VALUE = "%s";
  private static final String FORECAST_COST_VALUE = "$%s";
  private static final String TOTAL_COST_DATE_PATTERN = "dd MMMM, yyyy";
  private static final String DEFAULT_TIME_ZONE = "America/Los_Angeles";
  private static final String EMPTY_VALUE = "-";
  private static final String NA_VALUE = "NA";
  private static final long ONE_DAY_MILLIS = 86400000;

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
      BigDecimal forecastCost = getForecastCost(billingAmountData, getEndInstant(filters));
      return QLBillingTrendStats.builder()
          .totalCost(getTotalBillingStats(billingAmountData, filters))
          .costTrend(getBillingTrend(accountId, totalBillingAmount, forecastCost, aggregateFunction, filters))
          .forecastCost(getForecastBillingStats(forecastCost, getStartInstant(filters), getEndInstant(filters)))
          .build();
    } else {
      return QLBillingTrendStats.builder().build();
    }
  }

  public BigDecimal getForecastCost(QLBillingAmountData billingAmountData, Instant endInstant) {
    Preconditions.checkNotNull(billingAmountData);
    Instant currentTime = Instant.now();
    if (currentTime.isAfter(endInstant)) {
      return null;
    }

    long billingTimeDiffMillis = ONE_DAY_MILLIS;
    if (billingAmountData.getMaxStartTime() != billingAmountData.getMinStartTime()) {
      billingTimeDiffMillis = billingAmountData.getMaxStartTime() - billingAmountData.getMinStartTime();
    }

    BigDecimal totalBillingAmount = billingAmountData.getCost();
    BigDecimal slope = totalBillingAmount.divide(new BigDecimal(billingTimeDiffMillis), 2, RoundingMode.HALF_UP);

    long actualTimeDiffMillis = endInstant.toEpochMilli() - billingAmountData.getMinStartTime();
    return slope.multiply(new BigDecimal(actualTimeDiffMillis));
  }

  public Instant getEndInstant(List<QLBillingDataFilter> filters) {
    return Instant.ofEpochMilli(getEndTimeFilter(filters).getValue().longValue());
  }

  public Instant getStartInstant(List<QLBillingDataFilter> filters) {
    return Instant.ofEpochMilli(getStartTimeFilter(filters).getValue().longValue());
  }

  private QLBillingStatsInfo getForecastBillingStats(
      BigDecimal forecastCost, Instant startInstant, Instant endInstant) {
    String forecastCostDescription = EMPTY_VALUE;
    String forecastCostValue = EMPTY_VALUE;
    if (forecastCost != null) {
      String startInstantFormat = getTotalCostFormattedDate(startInstant);
      String endInstantFormat = getTotalCostFormattedDate(endInstant);
      forecastCostDescription = String.format(FORECAST_COST_DESCRIPTION, startInstantFormat, endInstantFormat);
      forecastCostValue = String.format(FORECAST_COST_VALUE, getRoundedDoubleValue(forecastCost));
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(FORECAST_COST_LABEL)
        .statsDescription(forecastCostDescription)
        .statsValue(forecastCostValue)
        .build();
  }

  private List<QLBillingDataFilter> getTrendFilter(
      List<QLBillingDataFilter> filters, Instant startInstant, Instant endInstant) {
    long diffMillis = endInstant.toEpochMilli() - startInstant.toEpochMilli();
    long trendStartTime = startInstant.toEpochMilli() - diffMillis - ONE_DAY_MILLIS;
    long trendEndTime = startInstant.toEpochMilli() - ONE_DAY_MILLIS;
    return updateTimeFilter(filters, trendStartTime, trendEndTime);
  }

  private List<QLBillingDataFilter> updateTimeFilter(List<QLBillingDataFilter> filters, long startTime, long endTime) {
    List<QLBillingDataFilter> entityFilters =
        filters.stream()
            .filter(qlBillingDataFilter
                -> null == qlBillingDataFilter.getStartTime() && null == qlBillingDataFilter.getEndTime())
            .collect(Collectors.toList());
    entityFilters.add(QLBillingDataFilter.builder()
                          .startTime(QLTimeFilter.builder().value(startTime).operator(QLTimeOperator.AFTER).build())
                          .build());
    entityFilters.add(QLBillingDataFilter.builder()
                          .endTime(QLTimeFilter.builder().value(endTime).operator(QLTimeOperator.BEFORE).build())
                          .build());
    return entityFilters;
  }

  private QLBillingStatsInfo getTotalBillingStats(
      QLBillingAmountData billingAmountData, List<QLBillingDataFilter> filters) {
    Instant startInstant = Instant.ofEpochMilli(getStartTimeFilter(filters).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(billingAmountData.getMaxStartTime());
    String startInstantFormat = getTotalCostFormattedDate(startInstant);
    String endInstantFormat = getTotalCostFormattedDate(endInstant);
    String totalCostDescription = String.format(TOTAL_COST_DESCRIPTION, startInstantFormat, endInstantFormat);
    String totalCostValue = String.format(TOTAL_COST_VALUE, getRoundedDoubleValue(billingAmountData.getCost()));
    return QLBillingStatsInfo.builder()
        .statsLabel(TOTAL_COST_LABEL)
        .statsDescription(totalCostDescription)
        .statsValue(totalCostValue)
        .build();
  }

  private QLTimeFilter getStartTimeFilter(List<QLBillingDataFilter> filters) {
    Optional<QLBillingDataFilter> startTimeDataFilter =
        filters.stream().filter(qlBillingDataFilter -> qlBillingDataFilter.getStartTime() != null).findFirst();
    if (startTimeDataFilter.isPresent()) {
      return startTimeDataFilter.get().getStartTime();
    } else {
      throw new InvalidRequestException("Start time cannot be null");
    }
  }

  private QLTimeFilter getEndTimeFilter(List<QLBillingDataFilter> filters) {
    Optional<QLBillingDataFilter> endTimeDataFilter =
        filters.stream().filter(qlBillingDataFilter -> qlBillingDataFilter.getEndTime() != null).findFirst();
    if (endTimeDataFilter.isPresent()) {
      return endTimeDataFilter.get().getEndTime();
    } else {
      throw new InvalidRequestException("End time cannot be null");
    }
  }

  private String getTotalCostFormattedDate(Instant instant) {
    return getFormattedDate(instant, TOTAL_COST_DATE_PATTERN);
  }

  private String getFormattedDate(Instant instant, String datePattern) {
    return instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)).format(DateTimeFormatter.ofPattern(datePattern));
  }

  private QLBillingStatsInfo getBillingTrend(String accountId, BigDecimal totalBillingAmount, BigDecimal forecastCost,
      QLCCMAggregationFunction aggregateFunction, List<QLBillingDataFilter> filters) {
    List<QLBillingDataFilter> trendFilters = getTrendFilter(filters, getStartInstant(filters), getEndInstant(filters));
    QLBillingAmountData prevBillingAmountData = getBillingAmountData(accountId, aggregateFunction, trendFilters);
    Instant filterStartTime = Instant.ofEpochMilli(getStartTimeFilter(trendFilters).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(getEndTimeFilter(trendFilters).getValue().longValue());
    String trendCostDescription = String.format(TREND_COST_DESCRIPTION, getRoundedDoubleValue(totalBillingAmount),
        getTotalCostFormattedDate(filterStartTime), getTotalCostFormattedDate(endInstant));
    String trendCostValue = NA_VALUE;
    if (prevBillingAmountData != null && prevBillingAmountData.getCost().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal prevTotalBillingAmount = prevBillingAmountData.getCost();
      Instant startInstant = Instant.ofEpochMilli(prevBillingAmountData.getMinStartTime());
      String startInstantFormat = getTotalCostFormattedDate(startInstant);
      String endInstantFormat = getTotalCostFormattedDate(endInstant);
      BigDecimal amountDifference = totalBillingAmount.subtract(prevTotalBillingAmount);
      if (null != forecastCost) {
        amountDifference = forecastCost.subtract(prevTotalBillingAmount);
      }
      trendCostDescription = String.format(
          TREND_COST_DESCRIPTION, getRoundedDoubleValue(amountDifference), startInstantFormat, endInstantFormat);
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
