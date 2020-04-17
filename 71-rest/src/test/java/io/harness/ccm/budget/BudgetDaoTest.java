package io.harness.ccm.budget;

import static io.harness.ccm.budget.entities.BudgetType.PREVIOUS_MONTH_SPEND;
import static io.harness.ccm.budget.entities.BudgetType.SPECIFIED_AMOUNT;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.EnvironmentType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.List;

public class BudgetDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String applicationId1 = "APPLICATION_ID_1";
  private String applicationId2 = "APPLICATION_ID_2";
  private Budget budget1;
  private Budget budget2;

  @Inject private BudgetDao budgetDao;

  @Before
  public void setUp() {
    budget1 = Budget.builder().accountId(accountId).type(PREVIOUS_MONTH_SPEND).build();

    budget2 = Budget.builder()
                  .accountId(accountId)
                  .name("test_budget")
                  .scope(ApplicationBudgetScope.builder()
                             .applicationIds(new String[] {applicationId1, applicationId2})
                             .environmentType(EnvironmentType.ALL)
                             .build())
                  .type(SPECIFIED_AMOUNT)
                  .budgetAmount(100.0)
                  .build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testSave() {
    String budgetId = budgetDao.save(budget1);
    assertThat(budgetId).isNotNull();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGet() {
    String budgetId = budgetDao.save(budget1);
    Budget budget = budgetDao.get(budgetId);
    assertThat(budget.getUuid()).isEqualTo(budgetId);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldListAllBudgets() {
    budgetDao.save(budget1);
    budgetDao.save(budget2);
    List<Budget> budgets = budgetDao.list(accountId, 0, 0);
    assertThat(budgets).hasSize(2);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldListPaginatedBudgets() {
    budgetDao.save(budget1);
    budgetDao.save(budget2);
    List<Budget> budgets1 = budgetDao.list(accountId, 1, 0);
    assertThat(budgets1).hasSize(1);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testUpdate() {
    String budgetId1 = budgetDao.save(budget1);
    budgetDao.update(budgetId1, budget2);
    assertThat(budgetDao.get(budgetId1).getType()).isEqualTo(SPECIFIED_AMOUNT);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testDelete() {
    String budgetId = budgetDao.save(budget1);
    boolean result = budgetDao.delete(budgetId);
    assertThat(result).isTrue();
  }
}
