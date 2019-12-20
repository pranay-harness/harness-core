package io.harness.ccm.budget;

import static io.harness.ccm.budget.BudgetUtils.roundOffBudgetCreationTime;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.ClusterBudgetScope;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataQueryBuilder;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.datafetcher.budget.BudgetDefaultKeys;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData.QLBudgetTableDataBuilder;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableListData;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class BudgetServiceImpl implements BudgetService {
  @Inject DataFetcherUtils dataFetcherUtils;
  @Inject private BudgetDao budgetDao;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;

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
    int prevAlertThreshold = alertThresholds[thresholdIndex].getAlertsSent();
    alertThresholds[thresholdIndex].setAlertsSent(prevAlertThreshold + 1);
    budget.setAlertThresholds(alertThresholds);
    update(budget.getUuid(), budget);
  }

  @Override
  public Budget get(String budgetId) {
    return budgetDao.get(budgetId);
  }

  @Override
  public List<Budget> list(String accountId) {
    return budgetDao.list(accountId, 0, 0);
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
  public Double getActualCost(Budget budget) {
    QLBudgetTableListData data = getBudgetData(budget);
    return data.getData().get(0).getActualCost();
  }

  @Override
  public QLBudgetTableListData getBudgetData(Budget budget) {
    Preconditions.checkNotNull(budget.getAccountId());

    List<QLBillingDataFilter> filters = new ArrayList<>();
    if (budget.getScope().getClass().equals(ClusterBudgetScope.class)) {
      ClusterBudgetScope clusterBudgetScope = (ClusterBudgetScope) budget.getScope();
      filters.add(BudgetUtils.makeClusterFilter(clusterBudgetScope.getClusterIds()));
    } else if (budget.getScope().getClass().equals(ApplicationBudgetScope.class)) {
      ApplicationBudgetScope applicationBudgetScope = (ApplicationBudgetScope) budget.getScope();
      filters.add(BudgetUtils.makeApplicationFilter(applicationBudgetScope.getApplicationIds()));
    }

    QLCCMAggregationFunction aggregationFunction = BudgetUtils.makeBillingAmtAggregation();
    QLTimeSeriesAggregation groupBy = BudgetUtils.makeStartTimeEntityGroupBy();

    BillingDataQueryMetadata queryData = billingDataQueryBuilder.formBudgetInsightQuery(
        budget.getAccountId(), filters, aggregationFunction, groupBy, Collections.EMPTY_LIST);
    logger.info("BudgetDataFetcher query: {}", queryData.getQuery());

    ResultSet resultSet = null;
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      Double budgetedAmount = budget.getBudgetAmount();
      long createdAt = roundOffBudgetCreationTime(budget.getCreatedAt());
      return generateBudgetData(resultSet, queryData, budgetedAmount, createdAt);
    } catch (SQLException e) {
      logger.error("Exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }

    return null;
  }

  private QLBudgetTableListData generateBudgetData(ResultSet resultSet, BillingDataQueryMetadata queryData,
      Double budgetedAmount, long createdAt) throws SQLException {
    List<QLBudgetTableData> budgetTableDataList = new ArrayList<>();

    Double actualCost = BudgetDefaultKeys.ACTUAL_COST;
    long time = BudgetDefaultKeys.TIME;
    Double budgetVariance;
    Double budgetVariancePercentage;

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

      if (createdAt > time) {
        budgetedAmount = 0.0;
      }
      budgetVariance = budgetedAmount - actualCost;

      if (budgetedAmount != 0) {
        budgetVariancePercentage = (budgetVariance / budgetedAmount) * 100;
      } else {
        budgetVariancePercentage = 0.0;
      }

      final QLBudgetTableDataBuilder budgetTableDataBuilder = QLBudgetTableData.builder();
      budgetTableDataBuilder.actualCost(actualCost)
          .budgeted(budgetedAmount)
          .budgetVariance(budgetVariance)
          .budgetVariancePercentage(budgetVariancePercentage)
          .time(time);
      budgetTableDataList.add(budgetTableDataBuilder.build());
    }

    return QLBudgetTableListData.builder().data(budgetTableDataList).build();
  }
}
