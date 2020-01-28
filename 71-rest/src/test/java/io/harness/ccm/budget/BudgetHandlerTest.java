package io.harness.ccm.budget;

import static io.harness.ccm.budget.entities.BudgetType.SPECIFIED_AMOUNT;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.AlertThresholdBase;
import io.harness.ccm.budget.entities.ApplicationBudgetScope;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.EnvironmentType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.security.UserGroup;
import software.wings.service.impl.notifications.UserGroupBasedDispatcher;
import software.wings.service.intfc.UserGroupService;

public class BudgetHandlerTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String applicationId1 = "APPLICATION_ID_1";
  private String applicationId2 = "APPLICATION_ID_2";
  private AlertThreshold alertThreshold;
  private String[] userGroupIds = {"USER_GROUP_ID"};
  @Mock private UserGroup userGroup;
  private Budget budget;

  @Mock private BudgetService budgetService;
  @Mock private UserGroupService userGroupService;
  @Mock private UserGroupBasedDispatcher userGroupBasedDispatcher;
  @InjectMocks private BudgetHandler budgetHandler;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    alertThreshold = AlertThreshold.builder().percentage(0.5).basedOn(AlertThresholdBase.ACTUAL_COST).build();

    budget = Budget.builder()
                 .accountId(accountId)
                 .name("test_budget")
                 .scope(ApplicationBudgetScope.builder()
                            .applicationIds(new String[] {applicationId1, applicationId2})
                            .environmentType(EnvironmentType.ALL)
                            .build())
                 .type(SPECIFIED_AMOUNT)
                 .budgetAmount(0.0)
                 .alertThresholds(new AlertThreshold[] {alertThreshold})
                 .userGroupIds(userGroupIds)
                 .build();
    when(userGroupService.get(eq(accountId), anyString(), anyBoolean())).thenReturn(userGroup);
    doNothing().when(userGroupBasedDispatcher).dispatch(anyList(), isA(UserGroup.class));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testHandle() {
    budgetHandler.handle(budget);
    verify(budgetService).incAlertCount(any(Budget.class), anyInt());
    verify(userGroupBasedDispatcher).dispatch(anyList(), isA(UserGroup.class));
  }
}
