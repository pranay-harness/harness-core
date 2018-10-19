package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.TaskType.JENKINS;
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
import software.wings.api.JenkinsExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.DelegateTask;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsSubTaskType;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class JenkinsStateTest {
  private static final Activity ACTIVITY_WITH_ID = Activity.builder().build();

  static {
    ACTIVITY_WITH_ID.setUuid(ACTIVITY_ID);
  }
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ActivityService activityService;
  @Mock private ExecutionContextImpl executionContext;
  @Mock private DelegateService delegateService;
  @Mock private SecretManager secretManager;

  @InjectMocks private JenkinsState jenkinsState = new JenkinsState("jenkins");

  @Before
  public void setUp() throws Exception {
    jenkinsState.setJenkinsConfigId(SETTING_ID);
    jenkinsState.setJobName("testjob");
    when(executionContext.getApp()).thenReturn(anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).build());
    when(executionContext.getEnv()).thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(activityService.save(any(Activity.class))).thenReturn(ACTIVITY_WITH_ID);
    when(executionContext.getGlobalSettingValue(ACCOUNT_ID, SETTING_ID))
        .thenReturn(JenkinsConfig.builder()
                        .jenkinsUrl("http://jenkins")
                        .username("username")
                        .password("password".toCharArray())
                        .accountId(ACCOUNT_ID)
                        .build());
    when(executionContext.renderExpression(anyString()))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, String.class));
  }

  @Test
  public void shouldExecute() {
    ExecutionResponse executionResponse = jenkinsState.execute(executionContext);
    assertThat(executionResponse).isNotNull().hasFieldOrPropertyWithValue("async", true);
    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(delegateTaskArgumentCaptor.capture());
    assertThat(delegateTaskArgumentCaptor.getValue())
        .isNotNull()
        .hasFieldOrPropertyWithValue("taskType", JENKINS.name())
        .hasFieldOrProperty("parameters");
  }

  @Test
  public void shouldHandleAsyncResponseJenkinsStartTask() {
    when(executionContext.getStateExecutionData()).thenReturn(JenkinsExecutionData.builder().build());
    jenkinsState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            JenkinsExecutionResponse.builder()
                .errorMessage("Err")
                .executionStatus(ExecutionStatus.FAILED)
                .jenkinsResult("SUCCESS")
                .jobUrl("http://jenkins")
                .activityId(ACTIVITY_ID)
                .subTaskType(JenkinsSubTaskType.START_TASK)
                .filePathAssertionMap(Collections.emptyList())
                .build()));

    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  @Test
  public void shouldHandleAsyncResponseJenkinsPollTask() {
    when(executionContext.getStateExecutionData()).thenReturn(JenkinsExecutionData.builder().build());
    jenkinsState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            JenkinsExecutionResponse.builder()
                .errorMessage("Err")
                .executionStatus(ExecutionStatus.FAILED)
                .jenkinsResult("SUCCESS")
                .jobUrl("http://jenkins")
                .activityId(ACTIVITY_ID)
                .subTaskType(JenkinsSubTaskType.POLL_TASK)
                .filePathAssertionMap(Collections.emptyList())
                .build()));

    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  @Test
  public void shouldStartJenkinsPollTask() {
    when(executionContext.getStateExecutionData()).thenReturn(JenkinsExecutionData.builder().build());

    // Queued URL is empty, expected status to FAIL
    ExecutionResponse executionResponse = jenkinsState.startJenkinsPollTask(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            JenkinsExecutionResponse.builder()
                .errorMessage("Err")
                .executionStatus(ExecutionStatus.FAILED)
                .jenkinsResult("SUCCESS")
                .jobUrl("http://jenkins")
                .subTaskType(JenkinsSubTaskType.POLL_TASK)
                .filePathAssertionMap(Collections.emptyList())
                .build()));
    assertThat(executionResponse.getExecutionStatus().equals(ExecutionStatus.FAILED));

    // Set a Queued URL, expecting status to SUCCESS
    executionResponse = jenkinsState.startJenkinsPollTask(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            JenkinsExecutionResponse.builder()
                .errorMessage("Err")
                .executionStatus(ExecutionStatus.FAILED)
                .jenkinsResult("SUCCESS")
                .jobUrl("http://jenkins")
                .subTaskType(JenkinsSubTaskType.POLL_TASK)
                .queuedBuildUrl("http://jenkins")
                .filePathAssertionMap(Collections.emptyList())
                .timeElapsed(10000L)
                .build()));
    assertThat(executionResponse.getExecutionStatus().equals(ExecutionStatus.SUCCESS));
  }

  @Test
  public void shouldGetTimeout() {
    Integer timeoutMillis = jenkinsState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(Math.toIntExact(DEFAULT_ASYNC_CALL_TIMEOUT));
  }

  @Test
  public void shouldGetSetTimeout() {
    jenkinsState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = jenkinsState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }

  @Test
  public void shouldHandleAbort() {
    when(executionContext.getStateExecutionData())
        .thenReturn(JenkinsExecutionData.builder().activityId(ACTIVITY_ID).build());
    jenkinsState.handleAbortEvent(executionContext);
    assertThat(executionContext.getStateExecutionData()).isNotNull();
    assertThat(executionContext.getStateExecutionData().getErrorMsg()).isNotBlank();
  }
}
