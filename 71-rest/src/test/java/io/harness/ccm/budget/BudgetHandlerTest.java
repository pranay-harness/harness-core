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
import static org.mockito.Mockito.times;
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
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.impl.notifications.UserGroupBasedDispatcher;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.UserGroupService;

import java.util.Arrays;

public class BudgetHandlerTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String budgetId = "BUDGET_ID";
  private String applicationId1 = "APPLICATION_ID_1";
  private String applicationId2 = "APPLICATION_ID_2";
  private AlertThreshold alertThreshold;
  private String[] userGroupIds = {"USER_GROUP_ID"};
  private String memberId = "MEMBER_ID";
  private UserGroup userGroup;
  @Mock private User user;
  private Budget budget;

  @Mock private BudgetService budgetService;
  @Mock private UserGroupService userGroupService;
  @Mock private UserGroupBasedDispatcher userGroupBasedDispatcher;
  @Mock private UserServiceImpl userService;
  @Mock private EmailNotificationService emailNotificationService;
  @Mock private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @InjectMocks private BudgetHandler budgetHandler;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    alertThreshold = AlertThreshold.builder().percentage(0.5).basedOn(AlertThresholdBase.ACTUAL_COST).build();

    budget = Budget.builder()
                 .uuid(budgetId)
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
    userGroup = UserGroup.builder().accountId(accountId).memberIds(Arrays.asList(memberId)).build();
    when(userGroupService.get(eq(accountId), anyString(), anyBoolean())).thenReturn(userGroup);
    when(userService.get(anyString())).thenReturn(user);
    when(subdomainUrlHelper.getPortalBaseUrl(anyString())).thenReturn("BASE_URL");
    doNothing().when(userGroupBasedDispatcher).dispatch(anyList(), isA(UserGroup.class));
    when(emailNotificationService.send(any())).thenReturn(true);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldHandle() {
    budgetHandler.handle(budget);
    verify(budgetService).incAlertCount(any(Budget.class), anyInt());
    verify(emailNotificationService).send(any());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldNotHandleWithoutUserGroups() {
    budget.setUserGroupIds(null);
    budgetHandler.handle(budget);
    verify(emailNotificationService, times(0)).send(any());
  }
}
