package software.wings.graphql.datafetcher.budget;

import com.google.inject.Inject;

import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.ClusterBudgetScope;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingDataQueryBuilder;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.QLBillingStatsHelper;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeAggregationType;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData.QLBudgetTableDataBuilder;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableListData;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

@Slf4j
public class BudgetDataFetcher extends AbstractObjectDataFetcher<QLBudgetTableListData, QLBudgetQueryParameters> {
  public static final String BUDGET_DOES_NOT_EXIST_MSG = "Budget does not exist";
  @Inject HPersistence persistence;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject QLBillingStatsHelper statsHelper;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;

  private static final long DAY_IN_MILLI_SECONDS = 86400000L;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLBudgetTableListData fetch(QLBudgetQueryParameters qlQuery, String accountId) {
    Budget budget = null;
    if (qlQuery.getBudgetId() != null) {
      logger.info("Fetching budget data");
      budget = persistence.get(Budget.class, qlQuery.getBudgetId());
    }
    if (budget == null) {
      throw new InvalidRequestException(BUDGET_DOES_NOT_EXIST_MSG, WingsException.USER);
    }

    List<QLBillingDataFilter> filters = new ArrayList<>();
    if (budget.getScope().getClass().equals(ClusterBudgetScope.class)) {
      ClusterBudgetScope clusterBudgetScope = (ClusterBudgetScope) budget.getScope();
      filters.add(makeClusterFilter(clusterBudgetScope.getClusterIds()));
    } else if (budget.getScope().getClass().equals(ApplicationBudgetScope.class)) {
      ApplicationBudgetScope applicationBudgetScope = (ApplicationBudgetScope) budget.getScope();
      filters.add(makeApplicationFilter(applicationBudgetScope.getApplicationIds()));
    }

    QLCCMAggregationFunction aggregationFunction = makeBillingAmtAggregation();
    QLTimeSeriesAggregation groupBy = makeStartTimeEntityGroupBy();
    return getBudgetData(accountId, filters, aggregationFunction, groupBy, Collections.EMPTY_LIST, budget);
  }

  protected QLBudgetTableListData getBudgetData(@NotNull String accountId, List<QLBillingDataFilter> filters,
      QLCCMAggregationFunction aggregateFunction, QLTimeSeriesAggregation groupBy,
      List<QLBillingSortCriteria> sortCriteria, Budget budget) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    queryData =
        billingDataQueryBuilder.formBudgetInsightQuery(accountId, filters, aggregateFunction, groupBy, sortCriteria);
    logger.info("BudgetDataFetcher query!! {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateBudgetData(queryData, resultSet, budget);
    } catch (SQLException e) {
      logger.error("BudgetDataFetcher Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private QLBudgetTableListData generateBudgetData(
      BillingDataQueryMetadata queryData, ResultSet resultSet, Budget budget) throws SQLException {
    List<QLBudgetTableData> budgetTableDataList = new ArrayList<>();
    Double actualCost = BudgetDefaultKeys.ACTUAL_COST;
    Double budgetedAmount = budget.getBudgetAmount();
    long createdAt = roundOffBudgetCreationTime(budget.getCreatedAt());
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
            time = resultSet.getTimestamp(field.getFieldName(), utils.getDefaultCalendar()).getTime();
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

  private QLCCMAggregationFunction makeBillingAmtAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("billingamount")
        .build();
  }

  // Todo: add month aggregation
  private QLTimeSeriesAggregation makeStartTimeEntityGroupBy() {
    return QLTimeSeriesAggregation.builder()
        .timeAggregationType(QLTimeAggregationType.DAY)
        .timeAggregationValue(1)
        .build();
  }

  private QLBillingDataFilter makeClusterFilter(String[] values) {
    QLIdFilter clusterFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().cluster(clusterFilter).build();
  }

  private QLBillingDataFilter makeApplicationFilter(String[] values) {
    QLIdFilter applicationFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().application(applicationFilter).build();
  }

  // Todo: check round off is correct
  private long roundOffBudgetCreationTime(long createdAt) {
    LocalDate date = LocalDate.ofEpochDay(createdAt / DAY_IN_MILLI_SECONDS);
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }
}