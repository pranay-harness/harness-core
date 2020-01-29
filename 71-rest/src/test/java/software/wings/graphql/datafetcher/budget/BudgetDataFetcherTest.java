package software.wings.graphql.datafetcher.budget;

import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.Budget.BudgetBuilder;
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
import software.wings.graphql.schema.query.QLBudgetQueryParameters;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetDataList;
import software.wings.security.UserThreadLocal;

import java.sql.SQLException;

public class BudgetDataFetcherTest extends AbstractDataFetcherTest {
  @Mock BudgetService budgetService;
  @Mock private DataFetcherUtils utils;
  @InjectMocks BudgetDataFetcher budgetDataFetcher;

  QLBudgetDataList data;

  final String budgetId = "budgetId";
  final String accountId = "accountId";
  final String budgetName = "budgetName";
  final String[] clusterIds = {"clusterId"};
  final String[] appIds = {"appId"};
  final BudgetType budgetType = BudgetType.SPECIFIED_AMOUNT;
  final EnvironmentType environmentType = EnvironmentType.PROD;
  final long createdAt = System.currentTimeMillis();
  final long lastUpdatedAt = System.currentTimeMillis();
  final double budgetAmount = 25000.0;
  final QLBudgetQueryParameters queryParameters = new QLBudgetQueryParameters(budgetId);

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
    when(budgetService.getBudgetData(any(Budget.class))).thenReturn(data);
  }

  private Budget mockBudget(String scope) {
    BudgetBuilder budgetBuilder = Budget.builder()
                                      .uuid(budgetId)
                                      .accountId(accountId)
                                      .name(budgetName)
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
    assertThatThrownBy(() -> budgetDataFetcher.fetch(queryParameters, accountId))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testFetch() {
    when(budgetService.get(budgetId)).thenReturn(mockBudget("CLUSTER"));
    budgetDataFetcher.fetch(queryParameters, accountId);
    verify(budgetService).getBudgetData(any(Budget.class));
  }
}
