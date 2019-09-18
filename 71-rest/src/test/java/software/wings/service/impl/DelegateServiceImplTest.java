package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.service.intfc.AssignDelegateService;

import java.util.ArrayList;

public class DelegateServiceImplTest extends WingsBaseTest {
  private static final String VERSION = "1.0.0";
  @Mock private AssignDelegateService assignDelegateService;
  @Mock private Broadcaster broadcaster;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private DelegateTaskBroadcastHelper broadcastHelper;
  @InjectMocks @Inject private DelegateServiceImpl delegateService;

  @Before
  public void setUp() throws IllegalAccessException {
    when(broadcasterFactory.lookup(anyString(), anyBoolean())).thenReturn(broadcaster);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskWithPreAssignedDelegateId_Sync() throws Exception {
    when(assignDelegateService.pickFirstAttemptDelegate(any(DelegateTask.class))).thenReturn(DELEGATE_ID);

    DelegateTask delegateTask = getDelegateTask();
    when(broadcastHelper.broadcastNewDelegateTask(any())).thenReturn(delegateTask);
    delegateTask.setAsync(false);
    delegateService.saveDelegateTask(delegateTask, false);
    assertThat(delegateTask.getBroadcastCount()).isEqualTo(0);
    verify(broadcastHelper, times(0)).broadcastNewDelegateTask(any());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskWithPreAssignedDelegateId_Async() throws Exception {
    when(assignDelegateService.pickFirstAttemptDelegate(any(DelegateTask.class))).thenReturn(DELEGATE_ID);

    DelegateTask delegateTask = getDelegateTask();
    delegateTask.setAsync(true);
    DelegateTask task = delegateService.saveDelegateTask(delegateTask, true);
    assertThat(delegateTask.getBroadcastCount()).isEqualTo(0);
    verify(broadcastHelper, times(0)).broadcastNewDelegateTask(any());
  }

  private DelegateTask getDelegateTask() {
    return DelegateTask.builder()
        .async(false)
        .accountId(ACCOUNT_ID)
        .waitId(generateUuid())
        .appId(APP_ID)
        .version(VERSION)
        .data(TaskData.builder()
                  .taskType(TaskType.HTTP.name())
                  .parameters(new Object[] {})
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .tags(new ArrayList<>())
        .build();
  }
}
