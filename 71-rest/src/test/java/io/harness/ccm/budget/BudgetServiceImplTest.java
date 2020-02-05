package io.harness.ccm.budget;

import static io.harness.ccm.budget.entities.BudgetType.SPECIFIED_AMOUNT;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.AlertThresholdBase;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.Budget.BudgetBuilder;
import io.harness.ccm.budget.entities.BudgetType;
import io.harness.ccm.budget.entities.ClusterBudgetScope;
import io.harness.ccm.budget.entities.EnvironmentType;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataHelper;
import software.wings.graphql.datafetcher.billing.BillingDataQueryBuilder;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.BillingTrendStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.QLBillingAmountData;
import software.wings.graphql.datafetcher.billing.QLBillingStatsHelper;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetDataList;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

public class BudgetServiceImplTest extends CategoryTest {
  @Mock private TimeScaleDBService timeScaleDBService;
  @Mock private BudgetDao budgetDao;
  @Mock private BillingDataQueryBuilder billingDataQueryBuilder;
  @Mock private DataFetcherUtils utils;
  @Mock private BillingTrendStatsDataFetcher billingTrendStatsDataFetcher;
  @Mock private QLBillingStatsHelper statsHelper;
  @Mock private BillingDataHelper billingDataHelper;
  @InjectMocks BudgetServiceImpl budgetService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private String accountId = "ACCOUNT_ID";
  private String applicationId1 = "APPLICATION_ID_1";
  private String applicationId2 = "APPLICATION_ID_2";
  private String[] applicationIds = {applicationId1, applicationId2};
  private String[] clusterIds = {"CLUSTER_ID"};
  private String budgetId = "BUDGET_ID";
  private String budgetName = "BUDGET_NAME";
  private String entityName = "ENTITY_NAME";
  private BudgetType budgetType = BudgetType.SPECIFIED_AMOUNT;
  private double budgetAmount = 25000.0;
  final EnvironmentType environmentType = EnvironmentType.PROD;
  private long createdAt = System.currentTimeMillis();
  private long lastUpdatedAt = System.currentTimeMillis();

  final int[] count = {0};
  final double[] doubleVal = {0};

  final long currentTime = System.currentTimeMillis();
  final long[] calendar = {currentTime};
  private AlertThreshold alertThreshold;
  private Budget budget;

  @Mock BillingDataQueryMetadata queryData;
  @Mock Connection connection;
  @Mock Statement statement;
  @Mock ResultSet resultSet;

  @Before
  public void setUp() throws SQLException {
    alertThreshold = AlertThreshold.builder().percentage(0.5).basedOn(AlertThresholdBase.ACTUAL_COST).build();
    budget = Budget.builder()
                 .uuid(budgetId)
                 .accountId(accountId)
                 .name("test_budget")
                 .scope(ApplicationBudgetScope.builder()
                            .applicationIds(applicationIds)
                            .environmentType(io.harness.ccm.budget.entities.EnvironmentType.ALL)
                            .build())
                 .type(SPECIFIED_AMOUNT)
                 .budgetAmount(100.0)
                 .alertThresholds(new AlertThreshold[] {alertThreshold})
                 .build();
    when(billingDataQueryBuilder.formBudgetInsightQuery(anyString(), anyList(), any(QLCCMAggregationFunction.class),
             any(QLCCMTimeSeriesAggregation.class), anyList()))
        .thenReturn(queryData);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);
    resetValues();
    mockResultSet();
  }

  private Budget mockBudget(String scope) {
    BudgetBuilder budgetBuilder =
        Budget.builder()
            .uuid(budgetId)
            .accountId(accountId)
            .name(budgetName)
            .createdAt(createdAt)
            .lastUpdatedAt(lastUpdatedAt)
            .alertThresholds(new AlertThreshold[] {AlertThreshold.builder()
                                                       .percentage(0.5)
                                                       .alertsSent(1)
                                                       .crossedAt(currentTime)
                                                       .basedOn(AlertThresholdBase.ACTUAL_COST)
                                                       .build()})
            .type(budgetType)
            .budgetAmount(budgetAmount);
    if (scope.equals("CLUSTER")) {
      budgetBuilder.scope(ClusterBudgetScope.builder().clusterIds(clusterIds).build());
    } else {
      budgetBuilder.scope(
          ApplicationBudgetScope.builder().applicationIds(applicationIds).environmentType(environmentType).build());
    }
    return budgetBuilder.build();
  }

  private void mockResultSet() throws SQLException {
    when(resultSet.getDouble("COST")).thenAnswer((Answer<Double>) invocation -> 12500.0 + doubleVal[0]++);
    when(resultSet.getTimestamp(BillingDataMetaDataFields.TIME_SERIES.getFieldName(), utils.getDefaultCalendar()))
        .thenAnswer((Answer<Timestamp>) invocation -> {
          calendar[0] = calendar[0] + 3600000;
          return new Timestamp(calendar[0]);
        });
    returnResultSet(5);
  }

  private void resetValues() {
    count[0] = 0;
    doubleVal[0] = 0;
  }

  private void returnResultSet(int limit) throws SQLException {
    when(resultSet.next()).then((Answer<Boolean>) invocation -> {
      if (count[0] < limit) {
        count[0]++;
        return true;
      }
      return false;
    });
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldIncAlertCount() {
    budgetService.incAlertCount(budget, 0);
    ArgumentCaptor<Budget> argument = ArgumentCaptor.forClass(Budget.class);
    verify(budgetDao).update(eq(budgetId), argument.capture());
    assertThat(argument.getValue().getAlertThresholds()[0].getAlertsSent()).isEqualTo(1);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testSetThresholdCrossedTimestamp() {
    ArgumentCaptor<Budget> argument = ArgumentCaptor.forClass(Budget.class);
    long timestamp1 = Instant.now().toEpochMilli();
    budgetService.setThresholdCrossedTimestamp(budget, 0, timestamp1);
    verify(budgetDao).update(eq(budgetId), argument.capture());
    assertThat(argument.getValue().getAlertThresholds()[0].getCrossedAt()).isEqualTo(timestamp1);

    long timestamp2 = Instant.now().toEpochMilli();
    budgetService.setThresholdCrossedTimestamp(budget, 0, timestamp2);
    verify(budgetDao).update(eq(budgetId), argument.capture());
    assertThat(argument.getValue().getAlertThresholds()[0].getCrossedAt()).isEqualTo(timestamp1);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetBudgetDataForApplicationType() throws SQLException {
    when(budgetDao.get(budgetId)).thenReturn(mockBudget("APPLICATION"));
    QLBudgetDataList data = budgetService.getBudgetData(budget);
    verify(timeScaleDBService).getDBConnection();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetActualCost() throws Exception {
    budgetService.getActualCost(budget);
    verify(timeScaleDBService).getDBConnection();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetForecastCost() {
    when(billingTrendStatsDataFetcher.getBillingAmountData(
             eq(accountId), isA(QLCCMAggregationFunction.class), anyListOf(QLBillingDataFilter.class)))
        .thenReturn(QLBillingAmountData.builder().build());
    when(billingDataHelper.getEndInstant(anyListOf(QLBillingDataFilter.class))).thenReturn(Instant.now());
    budgetService.getForecastCost(budget);
    verify(billingTrendStatsDataFetcher)
        .getBillingAmountData(eq(accountId), isA(QLCCMAggregationFunction.class), anyListOf(QLBillingDataFilter.class));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldGetBudgetDetails() {
    when(statsHelper.getEntityName(any(), any())).thenReturn(entityName);
    QLBudgetTableData budgetDetails = budgetService.getBudgetDetails(mockBudget("APPLICATION"));
    assertThat(budgetDetails.getName()).isEqualTo(budgetName);
    assertThat(budgetDetails.getId()).isEqualTo(budgetId);
    assertThat(budgetDetails.getType()).isEqualTo(SPECIFIED_AMOUNT.toString());
    assertThat(budgetDetails.getActualAmount()).isEqualTo(0.0);
    assertThat(budgetDetails.getBudgetedAmount()).isEqualTo(budgetAmount);
    assertThat(budgetDetails.getScopeType()).isEqualTo("APPLICATION");
    assertThat(budgetDetails.getAppliesTo()[0]).isEqualTo(entityName);
    assertThat(budgetDetails.getAppliesTo()[1]).isEqualTo(entityName);

    // when budget scope is cluster
    budgetDetails = budgetService.getBudgetDetails(mockBudget("CLUSTER"));
    assertThat(budgetDetails.getName()).isEqualTo(budgetName);
    assertThat(budgetDetails.getId()).isEqualTo(budgetId);
    assertThat(budgetDetails.getType()).isEqualTo(SPECIFIED_AMOUNT.toString());
    assertThat(budgetDetails.getActualAmount()).isEqualTo(0.0);
    assertThat(budgetDetails.getBudgetedAmount()).isEqualTo(budgetAmount);
    assertThat(budgetDetails.getScopeType()).isEqualTo("CLUSTER");
    assertThat(budgetDetails.getAppliesTo()[0]).isEqualTo(entityName);
  }
}
