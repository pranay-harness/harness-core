package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.TaskType.BAMBOO;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.api.BambooExecutionData;
import software.wings.api.JenkinsExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class BambooStateTest {
  private static final Activity ACTIVITY_WITH_ID = Activity.builder().build();

  static {
    ACTIVITY_WITH_ID.setUuid(ACTIVITY_ID);
  }
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ActivityService activityService;
  @Mock private ExecutionContextImpl executionContext;
  @Mock private DelegateService delegateService;
  @Mock private SecretManager secretManager;

  @InjectMocks private BambooState bambooState = new BambooState("bamboo");

  @Before
  public void setUp() throws Exception {
    bambooState.setBambooConfigId(SETTING_ID);
    bambooState.setPlanName("TOD-TOD");
    when(executionContext.getApp()).thenReturn(anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).build());
    when(executionContext.getEnv()).thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(activityService.save(any(Activity.class))).thenReturn(ACTIVITY_WITH_ID);
    when(executionContext.getGlobalSettingValue(ACCOUNT_ID, SETTING_ID))
        .thenReturn(BambooConfig.builder()
                        .bambooUrl("http://bamboo")
                        .username("username")
                        .password("password".toCharArray())
                        .accountId(ACCOUNT_ID)
                        .build());
    when(executionContext.renderExpression(anyString()))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, String.class));
  }

  @Test
  public void shouldExecute() {
    ExecutionResponse executionResponse = bambooState.execute(executionContext);
    assertThat(executionResponse).isNotNull().hasFieldOrPropertyWithValue("async", true);
    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(delegateTaskArgumentCaptor.capture());
    assertThat(delegateTaskArgumentCaptor.getValue())
        .isNotNull()
        .hasFieldOrPropertyWithValue("taskType", BAMBOO.name())
        .hasFieldOrProperty("parameters");
  }

  @Test
  public void shouldHandleAsyncResponse() {
    when(executionContext.getStateExecutionData()).thenReturn(BambooExecutionData.builder().build());
    bambooState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            BambooState.BambooExecutionResponse.builder()
                .errorMessage("Err")
                .executionStatus(ExecutionStatus.FAILED)
                .buildStatus("SUCCESS")
                .planUrl("http://bamboo/TOD-TOD")
                .filePathAssertionMap(Collections.emptyList())
                .build()));

    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  @Test
  public void shouldGetTimeout() {
    Integer timeoutMillis = bambooState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(Math.toIntExact(DEFAULT_ASYNC_CALL_TIMEOUT));
  }

  @Test
  public void shouldGetSetTimeout() {
    bambooState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = bambooState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }

  @Test
  public void shouldHandleAbort() {
    when(executionContext.getStateExecutionData())
        .thenReturn(JenkinsExecutionData.builder().activityId(ACTIVITY_ID).build());
    bambooState.handleAbortEvent(executionContext);
    assertThat(executionContext.getStateExecutionData()).isNotNull();
    assertThat(executionContext.getStateExecutionData().getErrorMsg()).isNotBlank();
  }
}
