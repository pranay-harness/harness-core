package software.wings.graphql.datafetcher.budget;

import static io.harness.ccm.budget.entities.BudgetType.SPECIFIED_AMOUNT;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.BudgetServiceImpl;
import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.AlertThresholdBase;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.BudgetType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class BudgetListDataFetcherTest extends AbstractDataFetcherTest {
  @Mock BudgetServiceImpl budgetService;
  @InjectMocks BudgetListDataFetcher budgetListDataFetcher;

  private String accountId = "ACCOUNT_ID";
  private String applicationId1 = "APPLICATION_ID_1";
  private String applicationId2 = "APPLICATION_ID_2";
  private String[] applicationIds = {applicationId1, applicationId2};
  private String budgetId = "BUDGET_ID";
  private String budgetName = "BUDGET_NAME";
  private BudgetType budgetType = BudgetType.SPECIFIED_AMOUNT;
  private double budgetAmount = 25000.0;
  private Double[] alertAt = {0.5};

  private AlertThreshold alertThreshold;
  private Budget budget;
  private QLBudgetTableData budgetDetails;
  private QLBudgetQueryParameters queryParameters;

  @Before
  public void setUp() throws SQLException {
    alertThreshold = AlertThreshold.builder().percentage(0.5).basedOn(AlertThresholdBase.ACTUAL_COST).build();
    budget = Budget.builder()
                 .uuid(budgetId)
                 .accountId(accountId)
                 .name(budgetName)
                 .scope(ApplicationBudgetScope.builder()
                            .applicationIds(applicationIds)
                            .environmentType(io.harness.ccm.budget.entities.EnvironmentType.ALL)
                            .build())
                 .type(SPECIFIED_AMOUNT)
                 .budgetAmount(budgetAmount)
                 .alertThresholds(new AlertThreshold[] {alertThreshold})
                 .build();
    budgetDetails = QLBudgetTableData.builder()
                        .name(budget.getName())
                        .id(budget.getUuid())
                        .type(budget.getType().toString())
                        .scopeType("APPLICATION")
                        .appliesTo(applicationIds)
                        .alertAt(alertAt)
                        .budgetedAmount(budget.getBudgetAmount())
                        .actualAmount(1000.0)
                        .build();
    queryParameters = new QLBudgetQueryParameters(budgetId);
    when(budgetService.list(accountId)).thenReturn(Arrays.asList(budget));
    when(budgetService.getBudgetDetails(budget)).thenReturn(budgetDetails);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetch() {
    List<QLBudgetTableData> budgetTableData = budgetListDataFetcher.fetch(queryParameters, accountId);
    assertThat(budgetTableData).hasSize(1);
    assertThat(budgetTableData.get(0).getName()).isEqualTo(budgetName);
    assertThat(budgetTableData.get(0).getId()).isEqualTo(budgetId);
  }
}
