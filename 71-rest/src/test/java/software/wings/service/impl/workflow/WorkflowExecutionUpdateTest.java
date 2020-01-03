package software.wings.service.impl.workflow;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAMA;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.event.handler.impl.Utils;
import io.harness.event.handler.impl.segment.SegmentHandler;
import io.harness.event.handler.impl.segment.SegmentHelper;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.event.listener.EventListener;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.app.GeneralNotifyEventListener;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.events.TestUtils;
import software.wings.rules.Listeners;
import software.wings.service.impl.WorkflowExecutionUpdate;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.net.URISyntaxException;
import java.time.Duration;

/**
 * @author rktummala
 */
@Slf4j
@Listeners(GeneralNotifyEventListener.class)
public class WorkflowExecutionUpdateTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private PersistentLocker persistentLocker;
  @Inject private TestUtils testUtils;

  @InjectMocks @Inject private WorkflowExecutionUpdate workflowExecutionUpdate;

  private WorkflowExecution createNewWorkflowExecution(User triggeredBy) {
    WorkflowExecutionBuilder workflowExecutionBuilder = WorkflowExecution.builder()
                                                            .accountId(ACCOUNT_ID)
                                                            .appId(APP_ID)
                                                            .appName(APP_NAME)
                                                            .envType(NON_PROD)
                                                            .status(ExecutionStatus.EXPIRED)
                                                            .workflowType(WorkflowType.ORCHESTRATION)
                                                            .uuid(generateUuid());
    if (triggeredBy != null) {
      workflowExecutionBuilder.triggeredBy(EmbeddedUser.builder()
                                               .uuid(triggeredBy.getUuid())
                                               .email(triggeredBy.getEmail())
                                               .name(triggeredBy.getName())
                                               .build());
    }

    return workflowExecutionBuilder.build();
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldReportDeploymentEventToSegmentByTrigger() throws IllegalAccessException, URISyntaxException {
    SegmentConfig segmentConfig =
        SegmentConfig.builder().enabled(true).url("https://api.segment.io").apiKey("dummy_api_key").build();
    EventListener eventListener = mock(EventListener.class);
    SegmentHandler segmentHandler = Mockito.spy(new SegmentHandler(segmentConfig, eventListener));
    MainConfiguration mainConfiguration = mock(MainConfiguration.class);
    FieldUtils.writeField(mainConfiguration, "segmentConfig", segmentConfig, true);
    SegmentHelper segmentHelper = new SegmentHelper(mainConfiguration);
    FieldUtils.writeField(segmentHandler, "segmentHelper", segmentHelper, true);
    FieldUtils.writeField(workflowExecutionUpdate, "segmentHandler", segmentHandler, true);
    Account account = testUtils.createAccount();
    when(accountService.getFromCacheWithFallback(anyString())).thenReturn(account);
    WorkflowExecution workflowExecution = createNewWorkflowExecution(null);
    workflowExecutionUpdate.reportDeploymentEventToSegment(workflowExecution);
    verify(segmentHandler).reportTrackEvent(eq(account), anyString(), anyString(), anyMap(), anyMap());
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldReportDeploymentEventToSegmentByUser() throws IllegalAccessException, URISyntaxException {
    Account account = testUtils.createAccount();
    User user = testUtils.createUser(account);
    user.setSegmentIdentity(UUIDGenerator.generateUuid());
    SegmentConfig segmentConfig =
        SegmentConfig.builder().enabled(true).url("https://api.segment.io").apiKey("dummy_api_key").build();
    EventListener eventListener = mock(EventListener.class);
    UserService userService = mock(UserService.class);
    when(userService.get(anyString())).thenReturn(user);
    when(userService.getUserFromCacheOrDB(anyString())).thenReturn(user);
    when(userService.update(any(User.class))).thenReturn(user);
    SegmentHandler segmentHandler = Mockito.spy(new SegmentHandler(segmentConfig, eventListener));
    MainConfiguration mainConfiguration = mock(MainConfiguration.class);
    FieldUtils.writeField(mainConfiguration, "segmentConfig", segmentConfig, true);
    SegmentHelper segmentHelper = new SegmentHelper(mainConfiguration);
    Utils utils = spy(new Utils());
    FieldUtils.writeField(utils, "userService", userService, true);
    FieldUtils.writeField(segmentHandler, "segmentHelper", segmentHelper, true);
    FieldUtils.writeField(segmentHandler, "utils", utils, true);
    FieldUtils.writeField(segmentHandler, "userService", userService, true);
    FieldUtils.writeField(segmentHandler, "persistentLocker", persistentLocker, true);
    FieldUtils.writeField(workflowExecutionUpdate, "segmentHandler", segmentHandler, true);
    when(accountService.getFromCacheWithFallback(anyString())).thenReturn(account);
    AcquiredLock acquiredLock = mock(AcquiredLock.class);
    when(persistentLocker.waitToAcquireLock(anyString(), any(Duration.class), any(Duration.class)))
        .thenReturn(acquiredLock);
    WorkflowExecution workflowExecution = createNewWorkflowExecution(user);
    workflowExecutionUpdate.reportDeploymentEventToSegment(workflowExecution);
    verify(segmentHandler).reportTrackEvent(eq(account), anyString(), anyString(), anyMap(), anyMap());
  }
}
