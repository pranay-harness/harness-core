package io.harness.ccm.budget;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.AlertThresholdBase;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.BudgetScope;
import io.harness.ccm.budget.entities.BudgetScopeType;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import software.wings.features.CeBudgetFeature;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataHelper;
import software.wings.graphql.datafetcher.billing.BillingDataQueryBuilder;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BudgetServiceImpl implements BudgetService {
  @Inject private DataFetcherUtils dataFetcherUtils;
  @Inject private BudgetDao budgetDao;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject private BillingDataHelper billingDataHelper;
  @Inject QLBillingStatsHelper statsHelper;
  @Inject @Named(CeBudgetFeature.FEATURE_NAME) private UsageLimitedFeature ceBudgetFeature;
  @Inject BudgetUtils budgetUtils;
  private String NOTIFICATION_TEMPLATE = "%s | %s exceed %s ($%s)";
  private String DATE_TEMPLATE = "MM-dd-yyyy";
  private double BUDGET_AMOUNT_UPPER_LIMIT = 100000000;
  private String NO_BUDGET_AMOUNT_EXCEPTION = "Error in creating budget. No budget amount specified.";
  private String BUDGET_AMOUNT_NOT_WITHIN_BOUNDS_EXCEPTION =
      "Error in creating budget. The budget amount should be positive and less than 100 million dollars.";

  private static final long CACHE_SIZE = 10000;
  private static final int MAX_RETRY = 3;

  private LoadingCache<Budget, Double> budgetToCostCache = Caffeine.newBuilder()
                                                               .maximumSize(CACHE_SIZE)
                                                               .refreshAfterWrite(24, TimeUnit.HOURS)
                                                               .build(this ::computeActualCost);

  @Override
  public String create(Budget budget) {
    validateBudget(budget);
    removeEmailDuplicates(budget);
    return budgetDao.save(budget);
  }

  @Override
  public String clone(String budgetId, String cloneBudgetName) {
    Budget budget = budgetDao.get(budgetId);
    validateBudget(budget);
    Budget cloneBudget = Budget.builder()
                             .accountId(budget.getAccountId())
                             .name(cloneBudgetName)
                             .scope(budget.getScope())
                             .type(budget.getType())
                             .budgetAmount(budget.getBudgetAmount())
                             .alertThresholds(budget.getAlertThresholds())
                             .userGroupIds(budget.getUserGroupIds())
                             .build();
    return budgetDao.save(cloneBudget);
  }

  @Override
  public void update(String budgetId, Budget budget) {
    validateBudget(budget);
    removeEmailDuplicates(budget);
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
  public int getBudgetCount(String accountId) {
    return list(accountId).size();
  }

  @Override
  public boolean delete(String budgetId) {
    return budgetDao.delete(budgetId);
  }

  @Override
  public double getActualCost(Budget budget) {
    if (budget != null) {
      return budgetToCostCache.get(budget);
    }
    return 0;
  }

  @Override
  public double getForecastCost(Budget budget) {
    return budgetUtils.getForecastCost(budget);
  }

  @Override
  public QLBudgetDataList getBudgetData(Budget budget) {
    Preconditions.checkNotNull(budget.getAccountId());
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(budget.getScope().getBudgetScopeFilter());
    budgetUtils.addAdditionalFiltersBasedOnScope(budget, filters);
    filters.add(getFilterForCurrentBillingCycle());
    QLCCMAggregationFunction aggregationFunction = budgetUtils.makeBillingAmtAggregation();
    QLCCMTimeSeriesAggregation groupBy = budgetUtils.makeStartTimeEntityGroupBy();

    BillingDataQueryMetadata queryData = billingDataQueryBuilder.formBudgetInsightQuery(
        budget.getAccountId(), filters, aggregationFunction, groupBy, Collections.emptyList());
    logger.info("BudgetDataFetcher query: {}", queryData.getQuery());

    ResultSet resultSet = null;
    boolean successful = false;
    int retryCount = 0;
    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        Double budgetedAmount = budget.getBudgetAmount();
        return generateBudgetData(resultSet, queryData, budgetedAmount);
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          logger.error(
              "Failed to execute getBudgetData query in BudgetService, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), budget.getAccountId(), e);
        } else {
          logger.warn(
              "Failed to execute getBudgetData query in BudgetService, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), budget.getAccountId(), retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return null;
  }

  @Override
  public QLBudgetTableData getBudgetDetails(Budget budget) {
    List<Double> alertAt = new ArrayList<>();
    List<String> notificationMessages = new ArrayList<>();
    if (budget.getAlertThresholds() != null && budget.getBudgetAmount() != null) {
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
        .appliesToIds(getAppliesToIds(scope))
        .environment(environment)
        .alertAt(alertAt.toArray(new Double[0]))
        .notifications(notificationMessages.toArray(new String[0]))
        .budgetedAmount(budget.getBudgetAmount())
        .actualAmount(getActualCost(budget))
        .lastUpdatedAt(budget.getLastUpdatedAt())
        .build();
  }

  private QLBudgetDataList generateBudgetData(
      ResultSet resultSet, BillingDataQueryMetadata queryData, Double budgetedAmount) throws SQLException {
    List<QLBudgetData> budgetTableDataList = new ArrayList<>();
    if (budgetedAmount == null) {
      budgetedAmount = 0.0;
    }

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

      QLBudgetData qlBudgetData =
          QLBudgetData.builder()
              .actualCost(billingDataHelper.getRoundedDoubleValue(actualCost))
              .budgeted(billingDataHelper.getRoundedDoubleValue(budgetedAmount))
              .budgetVariance(billingDataHelper.getRoundedDoubleValue(budgetVariance))
              .budgetVariancePercentage(billingDataHelper.getRoundedDoubleValue(budgetVariancePercentage))
              .time(time)
              .build();
      budgetTableDataList.add(qlBudgetData);
    }
    return QLBudgetDataList.builder().data(budgetTableDataList).build();
  }

  private static Double getBudgetVariance(Double budgetedAmount, Double actualCost) {
    return actualCost - budgetedAmount;
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
    c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 1);
    long startTime = c.getTimeInMillis();
    return QLBillingDataFilter.builder()
        .startTime(QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(startTime).build())
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
    entityIds = getAppliesToIds(scope);
    List<String> entityNames = new ArrayList<>();
    BillingDataQueryMetadata.BillingDataMetaDataFields entityType =
        BillingDataQueryMetadata.BillingDataMetaDataFields.CLUSTERID;
    if (scope.getBudgetScopeFilter().getApplication() != null) {
      entityType = BillingDataQueryMetadata.BillingDataMetaDataFields.APPID;
    }
    for (String entityId : entityIds) {
      entityNames.add(statsHelper.getEntityName(entityType, entityId));
    }
    return entityNames.toArray(new String[0]);
  }

  private String[] getAppliesToIds(BudgetScope scope) {
    String[] entityIds = {};
    if (scope == null) {
      return entityIds;
    }
    if (scope.getBudgetScopeFilter().getCluster() != null) {
      entityIds = scope.getBudgetScopeFilter().getCluster().getValues();
    } else if (scope.getBudgetScopeFilter().getApplication() != null) {
      entityIds = scope.getBudgetScopeFilter().getApplication().getValues();
    }
    return entityIds;
  }

  private void validateBudget(Budget budget) {
    if (budget.getBudgetAmount() == null) {
      throw new InvalidRequestException(NO_BUDGET_AMOUNT_EXCEPTION);
    }
    if (budget.getBudgetAmount() < 0 || budget.getBudgetAmount() > BUDGET_AMOUNT_UPPER_LIMIT) {
      throw new InvalidRequestException(BUDGET_AMOUNT_NOT_WITHIN_BOUNDS_EXCEPTION);
    }
    int maxBudgetsAllowed = ceBudgetFeature.getMaxUsageAllowedForAccount(budget.getAccountId());
    int currentBudgetCount = getBudgetCount(budget.getAccountId());
    logger.info("Max budgets allowed : {} Current Count : {}", maxBudgetsAllowed, currentBudgetCount);
    if (currentBudgetCount >= maxBudgetsAllowed) {
      logger.info("Did not save Budget: '{}' for account ID {} because usage limit exceeded", budget.getName(),
          budget.getAccountId());
      throw new InvalidRequestException(
          String.format("Cannot create budget. Max budgets allowed for trial: %d", maxBudgetsAllowed));
    }
  }

  private void removeEmailDuplicates(Budget budget) {
    String[] emailAddresses = ArrayUtils.nullToEmpty(budget.getEmailAddresses());
    String[] uniqueEmailAddresses = new HashSet<>(Arrays.asList(emailAddresses)).toArray(new String[0]);
    budget.setEmailAddresses(uniqueEmailAddresses);
  }

  private double computeActualCost(Budget budget) {
    QLBudgetDataList data = getBudgetData(budget);
    if (data == null || data.getData().isEmpty()) {
      return 0;
    }
    return data.getData().get(data.getData().size() - 1).getActualCost();
  }

  @Override
  public boolean isStartOfMonth() {
    return budgetUtils.isStartOfMonth();
  }

  @Override
  public boolean isAlertSentInCurrentMonth(long crossedAt) {
    return budgetUtils.isAlertSentInCurrentMonth(crossedAt);
  }
}
