package io.harness.ccm.budget;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.AlertThresholdBase;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.BudgetScope;
import io.harness.ccm.budget.entities.BudgetScopeType;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataQueryBuilder;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.BillingTrendStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.QLBillingAmountData;
import software.wings.graphql.datafetcher.billing.QLBillingStatsHelper;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.datafetcher.budget.BudgetDefaultKeys;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetData;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetDataList;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
public class BudgetServiceImpl implements BudgetService {
  @Inject private DataFetcherUtils dataFetcherUtils;
  @Inject private BillingTrendStatsDataFetcher billingTrendStatsDataFetcher;
  @Inject private BudgetDao budgetDao;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject QLBillingStatsHelper statsHelper;
  private String NOTIFICATION_TEMPLATE = "%s | %s exceed %s ($%s)";
  private String DATE_TEMPLATE = "MM-DD-YYYY hh:mm a";

  @Override
  public String create(Budget budget) {
    return budgetDao.save(budget);
  }

  @Override
  public void update(String budgetId, Budget budget) {
    budgetDao.update(budgetId, budget);
  }

  @Override
  public void incAlertCount(Budget budget, int thresholdIndex) {
    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    int prevAlertsSent = alertThresholds[thresholdIndex].getAlertsSent();
    alertThresholds[thresholdIndex].setAlertsSent(prevAlertsSent + 1);
    budget.setAlertThresholds(alertThresholds);
    update(budget.getUuid(), budget);
  }

  @Override
  public void setThresholdCrossedTimestamp(Budget budget, int thresholdIndex, long crossedAt) {
    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    long prevCrossedAt = alertThresholds[thresholdIndex].getCrossedAt();
    if (prevCrossedAt > budget.getCreatedAt()) {
      logger.info("The budget with id={} has already crossed the threshold #{}.", budget.getUuid(), thresholdIndex);
      return;
    }
    alertThresholds[thresholdIndex].setCrossedAt(crossedAt);
    budget.setAlertThresholds(alertThresholds);
    update(budget.getUuid(), budget);
  }

  @Override
  public Budget get(String budgetId) {
    return budgetDao.get(budgetId);
  }

  @Override
  public List<Budget> list(String accountId) {
    return budgetDao.list(accountId, Integer.MAX_VALUE - 1, 0);
  }

  @Override
  public List<Budget> list(String accountId, Integer count, Integer startIndex) {
    return budgetDao.list(accountId, count, startIndex);
  }

  @Override
  public boolean delete(String budgetId) {
    return budgetDao.delete(budgetId);
  }

  @Override
  public double getActualCost(Budget budget) {
    QLBudgetDataList data = getBudgetData(budget);
    if (data == null || data.getData().isEmpty()) {
      return 0;
    }
    return data.getData().get(data.getData().size() - 1).getActualCost();
  }

  @Override
  public double getForecastCost(Budget budget) {
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(budget.getScope().getBudgetScopeFilter());
    filters.add(getEndTimeFilterForCurrentBillingCycle());
    QLCCMAggregationFunction aggregationFunction = BudgetUtils.makeBillingAmtAggregation();
    QLBillingAmountData billingAmountData =
        billingTrendStatsDataFetcher.getBillingAmountData(budget.getAccountId(), aggregationFunction, filters);
    Instant endInstant = billingTrendStatsDataFetcher.getEndInstant(filters);
    BigDecimal forecastCost = billingTrendStatsDataFetcher.getForecastCost(billingAmountData, endInstant);
    if (forecastCost == null) {
      return 0L;
    }
    return forecastCost.doubleValue();
  }

  @Override
  public QLBudgetDataList getBudgetData(Budget budget) {
    Preconditions.checkNotNull(budget.getAccountId());
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(budget.getScope().getBudgetScopeFilter());
    filters.add(getFilterForCurrentBillingCycle());
    QLCCMAggregationFunction aggregationFunction = BudgetUtils.makeBillingAmtAggregation();
    QLCCMTimeSeriesAggregation groupBy = BudgetUtils.makeStartTimeEntityGroupBy();

    BillingDataQueryMetadata queryData = billingDataQueryBuilder.formBudgetInsightQuery(
        budget.getAccountId(), filters, aggregationFunction, groupBy, Collections.emptyList());
    logger.info("BudgetDataFetcher query: {}", queryData.getQuery());

    ResultSet resultSet = null;
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      Double budgetedAmount = budget.getBudgetAmount();
      return generateBudgetData(resultSet, queryData, budgetedAmount);
    } catch (SQLException e) {
      logger.error("Exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  @Override
  public QLBudgetTableData getBudgetDetails(Budget budget) {
    List<Double> alertAt = new ArrayList<>();
    List<String> notificationMessages = new ArrayList<>();
    if (budget.getAlertThresholds() != null) {
      for (AlertThreshold alertThreshold : budget.getAlertThresholds()) {
        alertAt.add(alertThreshold.getPercentage());
        if (alertThreshold.getAlertsSent() != 0) {
          String costType =
              alertThreshold.getBasedOn() == AlertThresholdBase.ACTUAL_COST ? "Actual costs" : "Forecasted costs";
          SimpleDateFormat formatter = new SimpleDateFormat(DATE_TEMPLATE);
          Date date = new Date(alertThreshold.getCrossedAt());
          notificationMessages.add(String.format(NOTIFICATION_TEMPLATE, formatter.format(date), costType,
              alertThreshold.getPercentage() + "%", budget.getBudgetAmount()));
        }
      }
    }
    BudgetScope scope = budget.getScope();
    String scopeType = getScopeType(scope);
    String environment = "-";
    if (scopeType.equals(BudgetScopeType.APPLICATION) && scope != null) {
      ApplicationBudgetScope applicationBudgetScope = (ApplicationBudgetScope) scope;
      environment = applicationBudgetScope.getEnvironmentType().toString();
    }
    return QLBudgetTableData.builder()
        .name(budget.getName())
        .id(budget.getUuid())
        .type(budget.getType().toString())
        .scopeType(scopeType)
        .appliesTo(getAppliesTo(scope))
        .environment(environment)
        .alertAt(alertAt.toArray(new Double[0]))
        .notifications(notificationMessages.toArray(new String[0]))
        .budgetedAmount(budget.getBudgetAmount())
        .actualAmount(getActualCost(budget))
        .build();
  }

  private QLBudgetDataList generateBudgetData(
      ResultSet resultSet, BillingDataQueryMetadata queryData, Double budgetedAmount) throws SQLException {
    List<QLBudgetData> budgetTableDataList = new ArrayList<>();

    Double actualCost = BudgetDefaultKeys.ACTUAL_COST;
    long time = BudgetDefaultKeys.TIME;

    while (resultSet != null && resultSet.next()) {
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case SUM:
            actualCost = resultSet.getDouble(field.getFieldName());
            break;
          case TIME_SERIES:
            time = resultSet.getTimestamp(field.getFieldName(), dataFetcherUtils.getDefaultCalendar()).getTime();
            break;
          default:
            break;
        }
      }

      Double budgetVariance = getBudgetVariance(budgetedAmount, actualCost);

      Double budgetVariancePercentage = getBudgetVariancePercentage(budgetVariance, budgetedAmount);

      QLBudgetData qlBudgetData = QLBudgetData.builder()
                                      .actualCost(actualCost)
                                      .budgeted(budgetedAmount)
                                      .budgetVariance(budgetVariance)
                                      .budgetVariancePercentage(budgetVariancePercentage)
                                      .time(time)
                                      .build();
      budgetTableDataList.add(qlBudgetData);
    }
    return QLBudgetDataList.builder().data(budgetTableDataList).build();
  }

  private static Double getBudgetVariance(Double budgetedAmount, Double actualCost) {
    return budgetedAmount - actualCost;
  }

  private static Double getBudgetVariancePercentage(Double budgetVariance, Double budgetedAmount) {
    Double budgetVariancePercentage;
    if (budgetedAmount != 0) {
      budgetVariancePercentage = (budgetVariance / budgetedAmount) * 100;
    } else {
      budgetVariancePercentage = 0.0;
    }
    return budgetVariancePercentage;
  }

  private QLBillingDataFilter getFilterForCurrentBillingCycle() {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_MONTH, 1);
    long startTime = c.getTimeInMillis();
    return QLBillingDataFilter.builder()
        .startTime(QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(startTime).build())
        .build();
  }

  private QLBillingDataFilter getEndTimeFilterForCurrentBillingCycle() {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
    c.set(Calendar.HOUR, c.getActualMaximum(Calendar.HOUR));
    c.set(Calendar.MINUTE, c.getActualMaximum(Calendar.MINUTE));
    c.set(Calendar.SECOND, c.getActualMaximum(Calendar.SECOND));
    c.set(Calendar.MILLISECOND, c.getActualMaximum(Calendar.MILLISECOND));
    long endTime = c.getTimeInMillis();
    return QLBillingDataFilter.builder()
        .endTime(QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(endTime).build())
        .build();
  }

  private String getScopeType(BudgetScope scope) {
    String scopeType = "-";
    if (scope != null) {
      if (scope.getBudgetScopeFilter().getCluster() != null) {
        scopeType = BudgetScopeType.CLUSTER;
      } else {
        scopeType = BudgetScopeType.APPLICATION;
      }
    }
    return scopeType;
  }

  private String[] getAppliesTo(BudgetScope scope) {
    String[] entityIds = {};
    if (scope == null) {
      return entityIds;
    }
    List<String> entityNames = new ArrayList<>();
    BillingDataQueryMetadata.BillingDataMetaDataFields entityType =
        BillingDataQueryMetadata.BillingDataMetaDataFields.CLUSTERID;
    if (scope.getBudgetScopeFilter().getCluster() != null) {
      entityIds = scope.getBudgetScopeFilter().getCluster().getValues();
    } else if (scope.getBudgetScopeFilter().getApplication() != null) {
      entityIds = scope.getBudgetScopeFilter().getApplication().getValues();
      entityType = BillingDataQueryMetadata.BillingDataMetaDataFields.APPID;
    }
    for (String entityId : entityIds) {
      entityNames.add(statsHelper.getEntityName(entityType, entityId));
    }
    return entityNames.toArray(new String[0]);
  }
}
