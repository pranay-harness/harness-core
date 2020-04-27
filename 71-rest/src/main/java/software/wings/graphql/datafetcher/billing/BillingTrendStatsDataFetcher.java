package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
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
public class BillingTrendStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject BillingDataHelper billingDataHelper;
  @Inject private IdleCostTrendStatsDataFetcher idleCostTrendStatsDataFetcher;

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
  private static final String IDLE_COST_DESCRIPTION = "%s of total";
  private static final String IDLE_COST_LABEL = "Idle Cost";
  private static final String UNALLOCATED_COST_DESCRIPTION = "%s of total";
  private static final String UNALLOCATED_COST_LABEL = "Unallocated Cost";
  private static final String UTILIZED_COST_DESCRIPTION = "%s of total";
  private static final String UTILIZED_COST_LABEL = "Utilized Cost";
  private static final String COST_VALUE = "$%s";
  private static final String EMPTY_VALUE = "-";
  private static final String NA_VALUE = "NA";

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, aggregateFunction, filters);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingTrendStatsDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while billing data", e);
    }
  }

  protected QLBillingTrendStats getData(
      @NotNull String accountId, List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    QLTrendStatsCostData billingAmountData = getBillingAmountData(accountId, aggregateFunction, filters);
    if (billingAmountData != null && billingAmountData.getTotalCostData() != null) {
      BigDecimal totalBillingAmount = billingAmountData.getTotalCostData().getCost();
      BigDecimal forecastCost = billingDataHelper.getForecastCost(
          billingAmountData.getTotalCostData(), billingDataHelper.getEndInstant(filters));
      BigDecimal unallocatedCost = null;
      if (isUnallocatedCostRequired(aggregateFunction) && billingAmountData.getUnallocatedCostData() != null) {
        unallocatedCost = billingAmountData.getUnallocatedCostData().getCost();
      }
      return QLBillingTrendStats.builder()
          .totalCost(getTotalBillingStats(billingAmountData.getTotalCostData(), filters))
          .costTrend(getBillingTrend(accountId, totalBillingAmount, forecastCost, aggregateFunction, filters))
          .forecastCost(getForecastBillingStats(forecastCost, billingAmountData.getTotalCostData(),
              billingDataHelper.getStartInstant(filters), billingDataHelper.getEndInstant(filters)))
          .idleCost(getIdleCostStats(billingAmountData.getIdleCostData(), billingAmountData.getTotalCostData()))
          .unallocatedCost(getUnallocatedCostStats(unallocatedCost, billingAmountData.getTotalCostData()))
          .utilizedCost(getUtilizedCostStats(billingAmountData.getIdleCostData(), billingAmountData.getTotalCostData(),
              billingAmountData.getUnallocatedCostData()))
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

  private QLBillingStatsInfo getIdleCostStats(QLBillingAmountData idleCostData, QLBillingAmountData totalCostData) {
    String idleCostDescription = EMPTY_VALUE;
    String idleCostValue = EMPTY_VALUE;
    if (idleCostData != null) {
      idleCostValue = String.format(COST_VALUE, billingDataHelper.getRoundedDoubleValue(idleCostData.getCost()));
      if (totalCostData != null) {
        double percentageOfTotalCost = billingDataHelper.getRoundedDoublePercentageValue(
            BigDecimal.valueOf(idleCostData.getCost().doubleValue() / totalCostData.getCost().doubleValue()));
        idleCostDescription = String.format(IDLE_COST_DESCRIPTION, percentageOfTotalCost + "%",
            billingDataHelper.getRoundedDoubleValue(totalCostData.getCost()));
      }
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(IDLE_COST_LABEL)
        .statsDescription(idleCostDescription)
        .statsValue(idleCostValue)
        .build();
  }

  private QLBillingStatsInfo getUnallocatedCostStats(BigDecimal unallocatedCost, QLBillingAmountData totalCostData) {
    String unallocatedCostDescription = EMPTY_VALUE;
    String unallocatedCostValue = EMPTY_VALUE;
    if (unallocatedCost != null) {
      unallocatedCostValue = String.format(COST_VALUE, billingDataHelper.getRoundedDoubleValue(unallocatedCost));
      if (totalCostData != null) {
        double percentageOfTotalCost = billingDataHelper.getRoundedDoublePercentageValue(
            BigDecimal.valueOf(unallocatedCost.doubleValue() / totalCostData.getCost().doubleValue()));
        unallocatedCostDescription = String.format(UNALLOCATED_COST_DESCRIPTION, percentageOfTotalCost + "%",
            billingDataHelper.getRoundedDoubleValue(totalCostData.getCost()));
      }
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(UNALLOCATED_COST_LABEL)
        .statsDescription(unallocatedCostDescription)
        .statsValue(unallocatedCostValue)
        .build();
  }

  private QLBillingStatsInfo getUtilizedCostStats(
      QLBillingAmountData idleCostData, QLBillingAmountData totalCostData, QLBillingAmountData unallocatedCostData) {
    String utilizedCostDescription = EMPTY_VALUE;
    String utilizedCostValue = EMPTY_VALUE;
    if (idleCostData != null && totalCostData != null) {
      double utilizedCost = totalCostData.getCost().doubleValue() - idleCostData.getCost().doubleValue();
      if (unallocatedCostData != null) {
        utilizedCost -= unallocatedCostData.getCost().doubleValue();
      }
      utilizedCostValue = String.format(COST_VALUE, billingDataHelper.getRoundedDoubleValue(utilizedCost));
      double percentageOfTotalCost = billingDataHelper.getRoundedDoublePercentageValue(
          BigDecimal.valueOf(utilizedCost / totalCostData.getCost().doubleValue()));
      utilizedCostDescription = String.format(UTILIZED_COST_DESCRIPTION, percentageOfTotalCost + "%",
          billingDataHelper.getRoundedDoubleValue(totalCostData.getCost()));
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(UTILIZED_COST_LABEL)
        .statsDescription(utilizedCostDescription)
        .statsValue(utilizedCostValue)
        .build();
  }

  private QLBillingStatsInfo getBillingTrend(String accountId, BigDecimal totalBillingAmount, BigDecimal forecastCost,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    List<QLBillingDataFilter> trendFilters = billingDataHelper.getTrendFilter(
        filters, billingDataHelper.getStartInstant(filters), billingDataHelper.getEndInstant(filters));
    QLBillingAmountData prevBillingAmountData =
        getBillingAmountData(accountId, aggregateFunction, trendFilters).getTotalCostData();
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

  public QLTrendStatsCostData getBillingAmountData(
      String accountId, List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    BillingDataQueryMetadata queryData =
        billingDataQueryBuilder.formTrendStatsQuery(accountId, aggregateFunction, filters);
    String query = queryData.getQuery();
    logger.info("Billing data query {}", query);
    ResultSet resultSet = null;
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      return fetchBillingAmount(queryData, resultSet);
    } catch (SQLException e) {
      logger.error("BillingStatsTimeSeriesDataFetcher Error exception", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private QLTrendStatsCostData fetchBillingAmount(BillingDataQueryMetadata queryData, ResultSet resultSet)
      throws SQLException {
    QLBillingAmountData totalCostData = null;
    QLBillingAmountData idleCostData = null;
    QLBillingAmountData unallocatedCostData = null;
    while (null != resultSet && resultSet.next()) {
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case SUM:
            if (resultSet.getBigDecimal(field.getFieldName()) != null) {
              totalCostData =
                  QLBillingAmountData.builder()
                      .cost(resultSet.getBigDecimal(BillingDataMetaDataFields.SUM.getFieldName()))
                      .minStartTime(resultSet
                                        .getTimestamp(BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(),
                                            utils.getDefaultCalendar())
                                        .getTime())
                      .maxStartTime(resultSet
                                        .getTimestamp(BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(),
                                            utils.getDefaultCalendar())
                                        .getTime())
                      .build();
            }
            break;
          case IDLECOST:
            if (resultSet.getBigDecimal(field.getFieldName()) != null) {
              idleCostData = QLBillingAmountData.builder()
                                 .cost(resultSet.getBigDecimal(BillingDataMetaDataFields.IDLECOST.getFieldName()))
                                 .minStartTime(resultSet
                                                   .getTimestamp(BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(),
                                                       utils.getDefaultCalendar())
                                                   .getTime())
                                 .maxStartTime(resultSet
                                                   .getTimestamp(BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(),
                                                       utils.getDefaultCalendar())
                                                   .getTime())
                                 .build();
            }
            break;
          case UNALLOCATEDCOST:
            if (resultSet.getBigDecimal(field.getFieldName()) != null) {
              unallocatedCostData =
                  QLBillingAmountData.builder()
                      .cost(resultSet.getBigDecimal(BillingDataMetaDataFields.UNALLOCATEDCOST.getFieldName()))
                      .minStartTime(resultSet
                                        .getTimestamp(BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(),
                                            utils.getDefaultCalendar())
                                        .getTime())
                      .maxStartTime(resultSet
                                        .getTimestamp(BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(),
                                            utils.getDefaultCalendar())
                                        .getTime())
                      .build();
            }
            break;
          default:
        }
      }
    }
    return QLTrendStatsCostData.builder()
        .totalCostData(totalCostData)
        .idleCostData(idleCostData)
        .unallocatedCostData(unallocatedCostData)
        .build();
  }

  private boolean isUnallocatedCostRequired(List<QLCCMAggregationFunction> aggregateFunctions) {
    for (QLCCMAggregationFunction aggregationFunction : aggregateFunctions) {
      if (aggregationFunction.getColumnName().equalsIgnoreCase("UNALLOCATEDCOST")) {
        return true;
      }
    }
    return false;
  }

  private double getRoundedDoubleValue(BigDecimal value) {
    return Math.round(value.doubleValue() * 100D) / 100D;
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregations, List<QLBillingSortCriteria> sort, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
