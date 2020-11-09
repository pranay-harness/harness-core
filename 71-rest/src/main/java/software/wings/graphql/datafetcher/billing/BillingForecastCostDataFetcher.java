package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingTrendStats;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingForecastCostDataFetcher
    extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction, QLBillingDataFilter, QLCCMGroupBy,
        QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject BillingDataHelper billingDataHelper;

  private static final String EMPTY_VALUE = "-";
  private static final String FORECAST_COST_LABEL = "Forecasted total cost";
  private static final String FORECAST_COST_DESCRIPTION = "of %s - %s";
  private static final String FORECAST_COST_VALUE = "$%s";

  @Override
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, aggregateFunction, filters);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingForecastCostDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while billing data", e);
    }
  }

  protected QLBillingTrendStats getData(
      @NotNull String accountId, List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    Instant endInstantForForecastCost = billingDataHelper.getEndInstantForForecastCost(filters);
    BigDecimal forecastCost = billingDataHelper.getNewForecastCost(
        billingDataHelper
            .getBillingAmountData(accountId, aggregateFunction, billingDataHelper.getFiltersForForecastCost(filters))
            .getTotalCostData(),
        endInstantForForecastCost);

    return QLBillingTrendStats.builder()
        .forecastCost(getForecastBillingStats(forecastCost, billingDataHelper.getStartInstantForForecastCost(),
            endInstantForForecastCost.plus(1, ChronoUnit.SECONDS)))
        .build();
  }

  private QLBillingStatsInfo getForecastBillingStats(
      BigDecimal forecastCost, Instant startInstant, Instant endInstant) {
    String forecastCostDescription = EMPTY_VALUE;
    String forecastCostValue = EMPTY_VALUE;
    if (forecastCost != null) {
      boolean isYearRequired = billingDataHelper.isYearRequired(startInstant, endInstant);
      String startInstantFormat = billingDataHelper.getTotalCostFormattedDate(startInstant, isYearRequired);
      String endInstantFormat = billingDataHelper.getTotalCostFormattedDate(endInstant, isYearRequired);
      forecastCostDescription = String.format(FORECAST_COST_DESCRIPTION, startInstantFormat, endInstantFormat);
      forecastCostValue = String.format(
          FORECAST_COST_VALUE, billingDataHelper.formatNumber(billingDataHelper.getRoundedDoubleValue(forecastCost)));
    }
    return QLBillingStatsInfo.builder()
        .statsLabel(FORECAST_COST_LABEL)
        .statsDescription(forecastCostDescription)
        .statsValue(forecastCostValue)
        .build();
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
