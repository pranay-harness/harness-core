package software.wings.graphql.datafetcher.budget;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.AlertThresholdBase;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.Budget.BudgetBuilder;
import io.harness.ccm.budget.entities.BudgetScopeType;
import io.harness.ccm.budget.entities.BudgetType;
import io.harness.ccm.budget.entities.ClusterBudgetScope;
import io.harness.ccm.budget.entities.EnvironmentType;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataHelper;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTrendStats;
import software.wings.security.UserThreadLocal;

import java.sql.SQLException;

public class BudgetTrendStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Mock BudgetService budgetService;
  @Mock private DataFetcherUtils utils;
  @Mock BillingDataHelper billingDataHelper;
  @InjectMocks BudgetTrendStatsDataFetcher budgetTrendStatsDataFetcher;

  private QLBudgetTableData budgetDetails;
  private String budgetId = "budgetId";
  private String accountId = "accountId";
  private String budgetName = "budgetName";
  private String[] clusterIds = {"clusterId"};
  private String[] appIds = {"appId"};
  private Double[] alertAt = {50.0};
  private BudgetType budgetType = BudgetType.SPECIFIED_AMOUNT;
  private EnvironmentType environmentType = EnvironmentType.PROD;
  private AlertThreshold[] alertThresholds;
  private long createdAt = System.currentTimeMillis();
  private long lastUpdatedAt = System.currentTimeMillis();
  private double budgetAmount = 25000.0;
  private long alertSentAt = 15;
  private QLBudgetQueryParameters queryParameters;
  private static final String ACTUAL_COST_LABEL = "Actual vs. budgeted";
  private static final String FORECASTED_COST_LABEL = "Forecasted vs. budgeted";

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
    queryParameters = new QLBudgetQueryParameters(budgetId);
    budgetDetails = QLBudgetTableData.builder()
                        .name(budgetName)
                        .id(budgetId)
                        .type(budgetType.toString())
                        .scopeType("APPLICATION")
                        .appliesTo(appIds)
                        .environment(environmentType.toString())
                        .alertAt(alertAt)
                        .budgetedAmount(budgetAmount)
                        .actualAmount(0.0)
                        .build();

    alertThresholds = new AlertThreshold[] {AlertThreshold.builder()
                                                .crossedAt(alertSentAt)
                                                .basedOn(AlertThresholdBase.ACTUAL_COST)
                                                .alertsSent(1)
                                                .percentage(50.0)
                                                .build()};
    when(budgetService.getBudgetDetails(any(Budget.class))).thenReturn(budgetDetails);
    when(billingDataHelper.getRoundedDoubleValue(budgetAmount)).thenReturn(budgetAmount);
  }

  private Budget mockBudget(String scope) {
    BudgetBuilder budgetBuilder = Budget.builder()
                                      .uuid(budgetId)
                                      .accountId(accountId)
                                      .name(budgetName)
                                      .alertThresholds(alertThresholds)
                                      .createdAt(createdAt)
                                      .lastUpdatedAt(lastUpdatedAt)
                                      .type(budgetType)
                                      .budgetAmount(budgetAmount);
    if (scope.equals("CLUSTER")) {
      budgetBuilder.scope(ClusterBudgetScope.builder().clusterIds(clusterIds).build());
    } else {
      budgetBuilder.scope(
          ApplicationBudgetScope.builder().applicationIds(appIds).environmentType(environmentType).build());
    }
    return budgetBuilder.build();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldThrowWhenFetchWithInvalidBudgetId() {
    when(budgetService.get(budgetId)).thenReturn(null);
    assertThatThrownBy(() -> budgetTrendStatsDataFetcher.fetch(queryParameters, accountId))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetch() {
    when(budgetService.get(budgetId)).thenReturn(mockBudget(BudgetScopeType.APPLICATION));
    QLBudgetTrendStats budgetTrendStats = budgetTrendStatsDataFetcher.fetch(queryParameters, accountId);
    assertThat(budgetTrendStats.getTotalCost().getStatsLabel()).isEqualTo(ACTUAL_COST_LABEL);
    assertThat(budgetTrendStats.getTotalCost().getStatsValue()).isEqualTo("$0.0 / $25000.0");
    assertThat(budgetTrendStats.getForecastCost().getStatsLabel()).isEqualTo(FORECASTED_COST_LABEL);
    assertThat(budgetTrendStats.getForecastCost().getStatsValue()).isEqualTo("$0.0 / $25000.0");
    assertThat(budgetTrendStats.getBudgetDetails().getName()).isEqualTo(budgetName);
    assertThat(budgetTrendStats.getBudgetDetails().getId()).isEqualTo(budgetId);
    assertThat(budgetTrendStats.getBudgetDetails().getScopeType()).isEqualTo("APPLICATION");
    assertThat(budgetTrendStats.getBudgetDetails().getType()).isEqualTo(budgetType.toString());
    assertThat(budgetTrendStats.getBudgetDetails().getAppliesTo()[0]).isEqualTo("appId");
    assertThat(budgetTrendStats.getBudgetDetails().getEnvironment()).isEqualTo(environmentType.toString());
  }
}
