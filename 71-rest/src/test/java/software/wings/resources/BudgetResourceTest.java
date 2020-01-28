package software.wings.resources;

import static io.harness.rule.OwnerRule.HANTANG;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.utils.ResourceTestRule;

import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

public class BudgetResourceTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String budgetId = "BUDGET_ID";
  private Budget budget;

  private static BudgetService budgetService = mock(BudgetService.class);
  @ClassRule
  public static ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(new BudgetResource(budgetService)).build();

  @Before
  public void setUp() {
    budget = Budget.builder().accountId(accountId).scope(ApplicationBudgetScope.builder().build()).build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testSave() {
    RESOURCES.client()
        .target(format("/budgets/?accountId=%s", accountId))
        .request()
        .post(entity(budget, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Budget>>() {});
    verify(budgetService).create(eq(budget));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGet() {
    RESOURCES.client()
        .target(format("/budgets/%s/?accountId=%s", budgetId, accountId))
        .request()
        .get(new GenericType<RestResponse<Budget>>() {});
    verify(budgetService).get(eq(budgetId));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testList() {
    RESOURCES.client()
        .target(format("/budgets?accountId=%s&count=%d&startIndex=%d", accountId, 0, 0))
        .request()
        .get(new GenericType<RestResponse<List<Budget>>>() {});
    verify(budgetService).list(eq(accountId), eq(0), eq(0));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testUpdate() {
    RESOURCES.client()
        .target(format("/budgets/%s/?accountId=%s", budgetId, accountId))
        .request()
        .put(entity(budget, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Budget>>() {});
    verify(budgetService).update(eq(budgetId), isA(Budget.class));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testDelete() {
    RESOURCES.client().target(format("/budgets/%s/?accountId=%s", budgetId, accountId)).request().delete();
    verify(budgetService).delete(eq(budgetId));
  }
}
