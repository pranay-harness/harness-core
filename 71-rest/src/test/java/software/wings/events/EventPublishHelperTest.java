package software.wings.events;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.CV_CONFIG_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.STATE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.USER_GROUP_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.WHITELIST_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse.PageResponseBuilder;
import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.model.Event;
import io.harness.event.publisher.EventPublisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.EntityType;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.access.Whitelist;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WhitelistService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateExecutionInstance;
import software.wings.verification.CVConfiguration;
import software.wings.verification.apm.APMCVServiceConfiguration;

import java.util.Arrays;

/**
 * @author rktummala on 12/05/18
 */
public class EventPublishHelperTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private AppService appService;
  @Mock private EventPublisher eventPublisher;
  @Mock private DelegateService delegateService;
  @Mock private WorkflowService workflowService;
  @Mock private WorkflowExecutionService executionService;
  @Mock private StateExecutionService stateExecutionService;
  @Mock private CVConfigurationService cvConfigurationService;
  @Mock private UserGroupService userGroupService;
  @Mock private UserService userService;
  @Mock private WhitelistService whitelistService;
  @Inject private EventTestHelper eventTestHelper;

  private User user;
  private Account account;

  @InjectMocks @Inject private EventPublishHelper eventPublishHelper = spy(EventPublishHelper.class);

  @Before
  public void setup() {
    account = eventTestHelper.createAccount();
    MarketoConfig marketoConfig = eventTestHelper.initializeMarketoConfig();
    setInternalState(eventPublishHelper, "marketoConfig", marketoConfig);
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);
    when(accountService.getFromCache(ACCOUNT_ID)).thenReturn(account);
    when(accountService.save(any())).thenReturn(account);
    user = eventTestHelper.createUser(account);
  }

  @Test
  @Category(UnitTests.class)
  public void testSendFirstWorkflowEvent() {
    UserThreadLocal.set(user);
    try {
      when(workflowService.listWorkflows(any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse().build());
      eventPublishHelper.publishWorkflowCreatedEvent(WORKFLOW_ID, ACCOUNT_ID);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      Workflow workflow = WorkflowBuilder.aWorkflow().uuid("invalid").build();
      when(workflowService.listWorkflows(any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(workflow)).withTotal(1).build());
      eventPublishHelper.publishWorkflowCreatedEvent(WORKFLOW_ID, ACCOUNT_ID);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      workflow = WorkflowBuilder.aWorkflow().uuid(WORKFLOW_ID).build();
      when(workflowService.listWorkflows(any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(workflow)).withTotal(1).build());
      eventPublishHelper.publishWorkflowCreatedEvent(WORKFLOW_ID, ACCOUNT_ID);
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testSendFirstCV247Event() {
    UserThreadLocal.set(user);
    try {
      when(cvConfigurationService.listConfigurations(anyString(), any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse().build());
      eventPublishHelper.publishSetupCV247Event(ACCOUNT_ID, CV_CONFIG_ID);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      CVConfiguration cvConfiguration = new APMCVServiceConfiguration();
      cvConfiguration.setUuid("invalid");
      when(cvConfigurationService.listConfigurations(anyString(), any(PageRequest.class)))
          .thenReturn(
              PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(cvConfiguration)).withTotal(1).build());
      eventPublishHelper.publishSetupCV247Event(ACCOUNT_ID, CV_CONFIG_ID);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      cvConfiguration = new APMCVServiceConfiguration();
      cvConfiguration.setUuid(CV_CONFIG_ID);
      when(cvConfigurationService.listConfigurations(anyString(), any(PageRequest.class)))
          .thenReturn(
              PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(cvConfiguration)).withTotal(1).build());
      eventPublishHelper.publishSetupCV247Event(ACCOUNT_ID, CV_CONFIG_ID);
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testSendRBACEventForFirstUserGroup() {
    UserThreadLocal.set(user);
    try {
      when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean()))
          .thenReturn(PageResponseBuilder.aPageResponse().build());
      eventPublishHelper.publishSetupRbacEvent(ACCOUNT_ID, USER_GROUP_ID, EntityType.USER_GROUP);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      UserGroup userGroup = UserGroup.builder().uuid("invalid").name("userGroup1").build();
      when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean()))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(userGroup)).withTotal(1).build());
      eventPublishHelper.publishSetupRbacEvent(ACCOUNT_ID, USER_GROUP_ID, EntityType.USER_GROUP);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      userGroup = UserGroup.builder().uuid(USER_GROUP_ID).name("userGroup1").build();
      when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean()))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(userGroup)).withTotal(1).build());
      eventPublishHelper.publishSetupRbacEvent(ACCOUNT_ID, USER_GROUP_ID, EntityType.USER_GROUP);
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testSendRBACEventForFirstUser() {
    UserThreadLocal.set(user);
    try {
      when(userService.list(any(PageRequest.class), anyBoolean()))
          .thenReturn(PageResponseBuilder.aPageResponse().build());
      eventPublishHelper.publishSetupRbacEvent(ACCOUNT_ID, USER_ID, EntityType.USER);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      User user = User.Builder.anUser().withUuid("invalid").withEmail("invalid@abcd.com").build();
      when(userService.list(any(PageRequest.class), anyBoolean()))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(user)).withTotal(1).build());
      eventPublishHelper.publishSetupRbacEvent(ACCOUNT_ID, USER_ID, EntityType.USER);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      user = User.Builder.anUser().withUuid(USER_ID).withEmail("valid@abcd.com").build();
      when(userService.list(any(PageRequest.class), anyBoolean()))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(user)).withTotal(1).build());
      eventPublishHelper.publishSetupRbacEvent(ACCOUNT_ID, USER_ID, EntityType.USER);
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testSendWhitelistEvent() {
    UserThreadLocal.set(user);
    try {
      when(whitelistService.list(anyString(), any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse().build());
      eventPublishHelper.publishSetupIPWhitelistingEvent(ACCOUNT_ID, WHITELIST_ID);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      Whitelist whitelist = Whitelist.builder().uuid("invalid").build();
      when(whitelistService.list(anyString(), any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(whitelist)).withTotal(1).build());
      eventPublishHelper.publishSetupIPWhitelistingEvent(ACCOUNT_ID, WHITELIST_ID);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      whitelist = Whitelist.builder().uuid(WHITELIST_ID).build();
      when(whitelistService.list(anyString(), any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(whitelist)).withTotal(1).build());
      eventPublishHelper.publishSetupIPWhitelistingEvent(ACCOUNT_ID, WHITELIST_ID);
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testSend2FASetupEvent() {
    UserThreadLocal.set(user);
    try {
      eventPublishHelper.publishSetup2FAEvent(ACCOUNT_ID);
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testSendUserInviteEvent() {
    UserThreadLocal.set(user);
    try {
      eventPublishHelper.publishUserInviteFromAccountEvent(ACCOUNT_ID, "abcd@abcd.com");
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testSendInstalledDelegateEvent() {
    UserThreadLocal.set(user);
    try {
      when(delegateService.list(any(PageRequest.class))).thenReturn(PageResponseBuilder.aPageResponse().build());
      eventPublishHelper.publishInstalledDelegateEvent(ACCOUNT_ID, DELEGATE_ID);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      Delegate delegate = aDelegate().withUuid("invalid").build();
      when(delegateService.list(any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(delegate)).withTotal(1).build());
      eventPublishHelper.publishInstalledDelegateEvent(ACCOUNT_ID, DELEGATE_ID);
      verify(eventPublisher, never()).publishEvent(any(Event.class));

      delegate = aDelegate().withUuid(DELEGATE_ID).build();
      when(delegateService.list(any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(delegate)).withTotal(1).build());
      eventPublishHelper.publishInstalledDelegateEvent(ACCOUNT_ID, DELEGATE_ID);
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testConfirmUserRegistrationEvent() {
    UserThreadLocal.set(user);
    try {
      User newUser = User.Builder.anUser().withEmail("abcd@abcd.com").build();
      eventPublishHelper.publishUserRegistrationCompletionEvent(ACCOUNT_ID, newUser);
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testFirstDeploymentEvent() {
    UserThreadLocal.set(user);
    try {
      when(userService.getUserFromCacheOrDB(user.getUuid())).thenReturn(user);
      when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
      when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(Arrays.asList(APP_ID));
      WorkflowExecution workflowExecution =
          WorkflowExecution.builder()
              .uuid(WORKFLOW_EXECUTION_ID)
              .createdBy(EmbeddedUser.builder().email("abcd@abcd.com").uuid(user.getUuid()).build())
              .appId(APP_ID)
              .build();
      when(executionService.listExecutions(any(PageRequest.class), anyBoolean()))
          .thenReturn(
              PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(workflowExecution)).withTotal(1).build());
      eventPublishHelper.handleDeploymentCompleted(workflowExecution);
      verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testFirstRollbackEvent() {
    UserThreadLocal.set(user);
    try {
      when(userService.getUserFromCacheOrDB(user.getUuid())).thenReturn(user);
      when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
      when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(Arrays.asList(APP_ID));
      WorkflowExecution workflowExecution =
          WorkflowExecution.builder()
              .uuid(WORKFLOW_EXECUTION_ID)
              .appId(APP_ID)
              .createdBy(EmbeddedUser.builder().email("abcd@abcd.com").uuid(user.getUuid()).build())
              .build();
      when(executionService.getExecutionWithoutSummary(APP_ID, WORKFLOW_EXECUTION_ID)).thenReturn(workflowExecution);
      when(executionService.listExecutions(any(PageRequest.class), anyBoolean()))
          .thenReturn(
              PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(workflowExecution)).withTotal(1).build());
      StateExecutionInstance stateExecutionInstance = StateExecutionInstance.Builder.aStateExecutionInstance()
                                                          .uuid(STATE_EXECUTION_ID)
                                                          .executionUuid(WORKFLOW_EXECUTION_ID)
                                                          .build();
      when(stateExecutionService.list(any(PageRequest.class)))
          .thenReturn(PageResponseBuilder.aPageResponse()
                          .withResponse(Arrays.asList(stateExecutionInstance))
                          .withTotal(1)
                          .build());
      eventPublishHelper.handleDeploymentCompleted(workflowExecution);
      verify(eventPublisher, times(3)).publishEvent(any(Event.class));
    } finally {
      UserThreadLocal.unset();
    }
  }
}
