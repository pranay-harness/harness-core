package io.harness.ccm.budget;

import static io.harness.ccm.budget.entities.BudgetType.PREVIOUS_MONTH_SPEND;
import static io.harness.ccm.budget.entities.BudgetType.SPECIFIED_AMOUNT;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Environment.EnvironmentType.ALL;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.List;

public class BudgetDaoTest extends WingsBaseTest {
  String accountId = "ACCOUNT_ID";
  String applicationId1 = "APPLICATION_ID_1";
  String applicationId2 = "APPLICATION_ID_2";
  Budget budget1;
  Budget budget2;

  @Inject private BudgetDao budgetDao;

  @Before
  public void setUp() {
    budget1 = Budget.builder().accountId(accountId).type(PREVIOUS_MONTH_SPEND).build();

    budget2 = Budget.builder()
                  .accountId(accountId)
                  .name("test_budget")
                  .scope(ApplicationBudgetScope.builder()
                             .applicationIds(new String[] {applicationId1, applicationId2})
                             .type(ALL)
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
    List<Budget> budgets2 = budgetDao.list(accountId, 0, 0);
    assertThat(budgets2).hasSize(2);
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
