package io.harness.ccm.budget;

import static io.harness.ccm.budget.entities.BudgetType.SPECIFIED_AMOUNT;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Environment.EnvironmentType.ALL;

import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.AlertThresholdBase;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class BudgetServiceImplTest {
  @Mock private BudgetDao budgetDao;
  @InjectMocks private BudgetServiceImpl budgetService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private String budgetId = "BUDGET_ID";
  private String accountId = "ACCOUNT_ID";
  private String applicationId1 = "APPLICATION_ID_1";
  private String applicationId2 = "APPLICATION_ID_2";
  private AlertThreshold alertThreshold;
  private Budget budget;

  @Before
  public void setUp() {
    alertThreshold = AlertThreshold.builder().percentage(0.5).basedOn(AlertThresholdBase.ACTUAL_COST).build();

    budget = Budget.builder()
                 .uuid(budgetId)
                 .accountId(accountId)
                 .name("test_budget")
                 .scope(ApplicationBudgetScope.builder()
                            .applicationIds(new String[] {applicationId1, applicationId2})
                            .type(ALL)
                            .build())
                 .type(SPECIFIED_AMOUNT)
                 .budgetAmount(100.0)
                 .alertThresholds(new AlertThreshold[] {alertThreshold})
                 .build();
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
}
