package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.JenkinsExecutionData.Builder.aJenkinsExecutionData;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.TaskType.JENKINS;
import static software.wings.sm.states.JenkinsState.JenkinsExecutionResponse.Builder.aJenkinsExecutionResponse;
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
import software.wings.CurrentThreadExecutor;
import software.wings.beans.Activity;
import software.wings.beans.DelegateTask;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

/**
 * Created by peeyushaggarwal on 10/25/16.
 */
public class JenkinsStateTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ExecutorService executorService;
  @Mock private ActivityService activityService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private ExecutionContextImpl executionContext;
  @Mock private DelegateService delegateService;

  @InjectMocks private JenkinsState jenkinsState = new JenkinsState("jenkins");

  @Before
  public void setUp() throws Exception {
    on(jenkinsState).set("executorService", new CurrentThreadExecutor());
    jenkinsState.setJenkinsConfigId(SETTING_ID);
    jenkinsState.setJobName("testjob");
    when(executionContext.getApp()).thenReturn(anApplication().withUuid(APP_ID).build());
    when(executionContext.getEnv()).thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(activityService.save(any(Activity.class))).thenReturn(anActivity().withUuid(ACTIVITY_ID).build());
    when(executionContext.getSettingValue(SETTING_ID, SettingVariableTypes.JENKINS.name()))
        .thenReturn(aJenkinsConfig()
                        .withJenkinsUrl("http://jenkins")
                        .withUsername("username")
                        .withPassword("password".toCharArray())
                        .withAccountId(ACCOUNT_ID)
                        .build());
    when(executionContext.renderExpression(anyString()))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, String.class));
  }

  @Test
  public void shouldExecute() throws Exception {
    ExecutionResponse executionResponse = jenkinsState.execute(executionContext);
    assertThat(executionResponse).isNotNull().hasFieldOrPropertyWithValue("async", true);
    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(delegateTaskArgumentCaptor.capture());
    assertThat(delegateTaskArgumentCaptor.getValue())
        .isNotNull()
        .hasFieldOrPropertyWithValue("taskType", JENKINS)
        .hasFieldOrProperty("parameters");
  }

  @Test
  public void shouldHandleAsyncResponse() throws Exception {
    when(executionContext.getStateExecutionData()).thenReturn(aJenkinsExecutionData().build());
    jenkinsState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            aJenkinsExecutionResponse()
                .withErrorMessage("Err")
                .withExecutionStatus(ExecutionStatus.FAILED)
                .withJenkinsResult("SUCCESS")
                .withJobUrl("http://jenkins")
                .withFilePathAssertionMap(Collections.emptyList())
                .build()));

    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }
}
