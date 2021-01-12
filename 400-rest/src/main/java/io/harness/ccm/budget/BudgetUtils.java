package io.harness.ccm.budget;

import static io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator.AFTER;
import static io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator.BEFORE;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.graphql.QLCEViewAggregateOperation;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewMetadataFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewTimeGroupType;
import io.harness.ccm.views.graphql.QLCEViewTimeSeriesData;
import io.harness.ccm.views.graphql.QLCEViewTimeTruncGroupBy;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;
import io.harness.ccm.views.service.impl.ViewsBillingServiceImpl;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.EnvironmentType;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;
import software.wings.graphql.datafetcher.billing.QLBillingAmountData;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.datafetcher.budget.BudgetTimescaleQueryHelper;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLTimeGroupType;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BudgetUtils {
  @Inject WingsPersistence wingsPersistence;
  @Inject BudgetTimescaleQueryHelper budgetTimescaleQueryHelper;
  @Inject BudgetDao budgetDao;
  @Inject ViewsBillingServiceImpl viewsBillingService;
  @Inject private BigQueryService bigQueryService;
  @Inject private CloudBillingHelper cloudBillingHelper;
  private String DEFAULT_TIMEZONE = "GMT";
  private static final long ONE_DAY_MILLIS = 86400000;
  private static final long OBSERVATION_PERIOD = 29 * ONE_DAY_MILLIS;

  public QLCCMAggregationFunction makeBillingAmtAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("billingamount")
        .build();
  }

  public QLCCMTimeSeriesAggregation makeStartTimeEntityGroupBy() {
    return QLCCMTimeSeriesAggregation.builder().timeGroupType(QLTimeGroupType.MONTH).build();
  }

  public double getForecastCost(Budget budget, String cloudProviderTable) {
    if (isPerspectiveBudget(budget)) {
      return getForecastCostForPerspectiveBudget(budget, cloudProviderTable);
    }
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(budget.getScope().getBudgetScopeFilter());
    addAdditionalFiltersBasedOnScope(budget, filters);
    filters.add(getStartTimeFilterForForecasting());
    filters.add(getEndOfMonthFilterForCurrentBillingCycle());
    QLBillingAmountData billingAmountData =
        budgetTimescaleQueryHelper.getBudgetCostData(budget.getAccountId(), makeBillingAmtAggregation(), filters);
    Instant endInstant = getEndInstant(filters);
    BigDecimal forecastCost = null;
    if (billingAmountData != null) {
      forecastCost = getNewForecastCost(billingAmountData, endInstant);
    }
    if (forecastCost == null) {
      return 0L;
    }
    return forecastCost.doubleValue();
  }

  public double getActualCost(Budget budget, String cloudProviderTable) {
    if (isPerspectiveBudget(budget)) {
      return getActualCostForPerspectiveBudget(budget, cloudProviderTable);
    }
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(budget.getScope().getBudgetScopeFilter());
    addAdditionalFiltersBasedOnScope(budget, filters);
    filters.add(getStartOfMonthFilterForCurrentBillingCycle());
    filters.add(getEndOfMonthFilterForCurrentBillingCycle());
    QLBillingAmountData billingAmountData =
        budgetTimescaleQueryHelper.getBudgetCostData(budget.getAccountId(), makeBillingAmtAggregation(), filters);
    if (billingAmountData == null) {
      return 0;
    }
    return billingAmountData.getCost().doubleValue();
  }

  public BigDecimal getNewForecastCost(QLBillingAmountData billingAmountData, Instant endInstant) {
    if (billingAmountData == null) {
      return null;
    }
    Instant currentTime = Instant.now();
    if (currentTime.isAfter(endInstant)) {
      return null;
    }

    BigDecimal totalBillingAmount = billingAmountData.getCost();
    long actualTimeDiffMillis =
        (endInstant.plus(1, ChronoUnit.SECONDS).toEpochMilli()) - billingAmountData.getMaxStartTime();

    long billingTimeDiffMillis = ONE_DAY_MILLIS;
    if (billingAmountData.getMaxStartTime() != billingAmountData.getMinStartTime()) {
      billingTimeDiffMillis =
          billingAmountData.getMaxStartTime() - billingAmountData.getMinStartTime() + ONE_DAY_MILLIS;
    }
    if (billingTimeDiffMillis < OBSERVATION_PERIOD) {
      return null;
    }

    return totalBillingAmount.multiply(
        new BigDecimal(actualTimeDiffMillis).divide(new BigDecimal(billingTimeDiffMillis), 2, RoundingMode.HALF_UP));
  }

  // Methods for adding various filters in budget total cost and forecast cost calculation

  public void addAdditionalFiltersBasedOnScope(Budget budget, List<QLBillingDataFilter> filters) {
    if (isApplicationScopePresent(budget)) {
      addEnvironmentIdFilter(budget, filters);
    } else {
      addInstanceTypeFilter(filters);
    }
  }

  private QLBillingDataFilter getStartOfMonthFilterForCurrentBillingCycle() {
    ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    LocalDate today = LocalDate.now(zoneId);
    ZonedDateTime zdtStart = today.withDayOfMonth(1).atStartOfDay(zoneId);
    long startOfMonth = zdtStart.toEpochSecond() * 1000;
    return QLBillingDataFilter.builder()
        .startTime(QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(startOfMonth).build())
        .build();
  }

  private QLBillingDataFilter getEndOfMonthFilterForCurrentBillingCycle() {
    ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    LocalDate today = LocalDate.now(zoneId);
    Calendar cal = Calendar.getInstance();
    int daysInMonth = cal.getActualMaximum(Calendar.DATE);
    ZonedDateTime zdtStart = today.withDayOfMonth(daysInMonth).atStartOfDay(zoneId);
    long endTime = zdtStart.toEpochSecond() * 1000 + ONE_DAY_MILLIS - 1000;
    return QLBillingDataFilter.builder()
        .endTime(QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(endTime).build())
        .build();
  }

  private QLBillingDataFilter getStartTimeFilterForForecasting() {
    long startTime = getStartOfCurrentDay() - 30 * ONE_DAY_MILLIS;
    return QLBillingDataFilter.builder()
        .startTime(QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(startTime).build())
        .build();
  }

  private boolean isApplicationScopePresent(Budget budget) {
    if (budget != null && budget.getScope().getBudgetScopeFilter().getApplication() != null) {
      return true;
    }
    return false;
  }

  private void addInstanceTypeFilter(List<QLBillingDataFilter> filters) {
    String[] instanceTypeValues = {"ECS_TASK_FARGATE", "ECS_CONTAINER_INSTANCE", "K8S_NODE"};
    filters.add(QLBillingDataFilter.builder()
                    .instanceType(QLIdFilter.builder().operator(QLIdOperator.IN).values(instanceTypeValues).build())
                    .build());
  }

  private void addEnvironmentIdFilter(Budget budget, List<QLBillingDataFilter> filters) {
    ApplicationBudgetScope scope = (ApplicationBudgetScope) budget.getScope();
    if (!scope.getEnvironmentType().toString().equals("ALL")) {
      String[] appIds = scope.getApplicationIds();
      String env = scope.getEnvironmentType().toString();
      EnvironmentType environmentType;
      if (env.equals("NON_PROD")) {
        environmentType = EnvironmentType.NON_PROD;
      } else {
        environmentType = EnvironmentType.PROD;
      }
      List<String> envIds = getEnvIdsByAppsAndType(Arrays.asList(appIds), environmentType);
      filters.add(
          QLBillingDataFilter.builder()
              .environment(QLIdFilter.builder().operator(QLIdOperator.IN).values(envIds.toArray(new String[0])).build())
              .build());
    }
  }

  public List<String> getEnvIdsByAppsAndType(List<String> appIds, EnvironmentType environmentType) {
    List<Environment> environments = wingsPersistence.createQuery(Environment.class)
                                         .field(EnvironmentKeys.appId)
                                         .in(appIds)
                                         .filter(EnvironmentKeys.environmentType, environmentType)
                                         .asList();
    return environments.stream().map(Environment::getUuid).collect(Collectors.toList());
  }

  // Utility methods related to start time and end time

  private Instant getEndInstant(List<QLBillingDataFilter> filters) {
    return Instant.ofEpochMilli(getEndTimeFilter(filters).getValue().longValue());
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

  public boolean isStartOfMonth() {
    long startOfDay = getStartOfCurrentDay();
    ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    LocalDate today = LocalDate.now(zoneId);
    ZonedDateTime zdtStart = today.withDayOfMonth(1).atStartOfDay(zoneId);
    long startOfMonth = zdtStart.toEpochSecond() * 1000;
    return startOfMonth == startOfDay;
  }

  private long getStartOfCurrentDay() {
    ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    LocalDate today = LocalDate.now(zoneId);
    ZonedDateTime zdtStart = today.atStartOfDay(zoneId);
    return zdtStart.toEpochSecond() * 1000;
  }

  // Method for checking if alert was sent in this month

  public boolean isAlertSentInCurrentMonth(long crossedAt) {
    ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    LocalDate today = LocalDate.now(zoneId);
    ZonedDateTime zdtStart = today.withDayOfMonth(1).atStartOfDay(zoneId);
    long startOfMonth = zdtStart.toEpochSecond() * 1000;
    long startOfDay = getStartOfCurrentDay();
    if (startOfDay == startOfMonth) {
      return true;
    }
    crossedAt -= ONE_DAY_MILLIS;
    return startOfMonth <= crossedAt;
  }

  public List<Budget> listBudgetsForAccount(String accountId) {
    return budgetDao.list(accountId);
  }

  public boolean isPerspectiveBudget(Budget budget) {
    return budget.getScope().getBudgetScopeFilter().getView() != null;
  }

  public QLCEViewFilterWrapper getPerspectiveTimeFilter(long timestamp, QLCEViewTimeFilterOperator operator) {
    return QLCEViewFilterWrapper.builder()
        .timeFilter(QLCEViewTimeFilter.builder()
                        .field(QLCEViewFieldInput.builder()
                                   .fieldId("startTime")
                                   .fieldName("startTime")
                                   .identifier(ViewFieldIdentifier.COMMON)
                                   .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
                                   .build())
                        .operator(operator)
                        .value(timestamp)
                        .build())
        .build();
  }

  public QLCEViewFilterWrapper getViewFilter(String viewId) {
    return QLCEViewFilterWrapper.builder()
        .viewMetadataFilter(QLCEViewMetadataFilter.builder().viewId(viewId).isPreview(false).build())
        .build();
  }

  public List<QLCEViewAggregation> getPerspectiveTotalCostAggregation() {
    return Collections.singletonList(
        QLCEViewAggregation.builder().columnName("cost").operationType(QLCEViewAggregateOperation.SUM).build());
  }

  public List<QLCEViewTimeSeriesData> getPerspectiveBudgetMonthlyCostData(
      String viewId, String accountId, long startTime) {
    List<QLCEViewAggregation> aggregationFunction = getPerspectiveTotalCostAggregation();
    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(
        QLCEViewGroupBy.builder()
            .timeTruncGroupBy(QLCEViewTimeTruncGroupBy.builder().resolution(QLCEViewTimeGroupType.MONTH).build())
            .build());
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getViewFilter(viewId));
    filters.add(getPerspectiveTimeFilter(startTime, AFTER));
    return viewsBillingService.convertToQLViewTimeSeriesData(
        viewsBillingService.getTimeSeriesStats(bigQueryService.get(), filters, groupBy, aggregationFunction,
            Collections.emptyList(), cloudBillingHelper.getCloudProviderTableName(accountId, unified)));
  }

  private double getActualCostForPerspectiveBudget(Budget budget, String cloudProviderTable) {
    String viewId = budget.getScope().getBudgetScopeFilter().getView().getValues()[0];
    long startTime = getStartOfMonthFilterForCurrentBillingCycle().getStartTime().getValue().longValue();
    long endTime = getEndOfMonthFilterForCurrentBillingCycle().getEndTime().getValue().longValue();
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getViewFilter(viewId));
    filters.add(getPerspectiveTimeFilter(startTime, AFTER));
    filters.add(getPerspectiveTimeFilter(endTime, BEFORE));

    if (cloudProviderTable == null) {
      cloudProviderTable = cloudBillingHelper.getCloudProviderTableName(budget.getAccountId(), unified);
    }

    QLCEViewTrendInfo trendData = viewsBillingService.getTrendStatsData(
        bigQueryService.get(), filters, getPerspectiveTotalCostAggregation(), cloudProviderTable);
    return trendData.getValue().doubleValue();
  }

  private double getForecastCostForPerspectiveBudget(Budget budget, String cloudProviderTable) {
    String viewId = budget.getScope().getBudgetScopeFilter().getView().getValues()[0];
    long startTime = getStartTimeFilterForForecasting().getStartTime().getValue().longValue();
    long endTime = getEndOfMonthFilterForCurrentBillingCycle().getEndTime().getValue().longValue();
    log.info("Start time for forecast cost: {}", startTime);
    log.info("End time for forecast cost: {}", endTime);
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getViewFilter(viewId));
    filters.add(getPerspectiveTimeFilter(startTime, AFTER));
    filters.add(getPerspectiveTimeFilter(endTime, BEFORE));

    if (cloudProviderTable == null) {
      cloudProviderTable = cloudBillingHelper.getCloudProviderTableName(budget.getAccountId(), unified);
    }

    QLCEViewTrendInfo trendData = viewsBillingService.getTrendStatsData(
        bigQueryService.get(), filters, getPerspectiveTotalCostAggregation(), cloudProviderTable);
    Instant endInstant = Instant.ofEpochMilli(endTime);
    QLBillingAmountData billingAmountData = QLBillingAmountData.builder()
                                                .cost(BigDecimal.valueOf(trendData.getValue().doubleValue()))
                                                .maxStartTime(getStartOfCurrentDay() - ONE_DAY_MILLIS)
                                                .minStartTime(startTime)
                                                .build();
    log.info("Billing data: {}", billingAmountData);
    BigDecimal forecastCost = null;
    if (billingAmountData != null) {
      forecastCost = getNewForecastCost(billingAmountData, endInstant);
    }
    if (forecastCost == null) {
      return 0L;
    }
    return forecastCost.doubleValue();
  }

  public boolean isBudgetBasedOnGivenView(Budget budget, String viewId) {
    if (isPerspectiveBudget(budget)) {
      return budget.getScope().getBudgetScopeFilter().getView().getValues()[0].equals(viewId);
    }
    return false;
  }
}
