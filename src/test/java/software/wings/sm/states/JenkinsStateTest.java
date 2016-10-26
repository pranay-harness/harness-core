package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.JenkinsExecutionData.Builder.aJenkinsExecutionData;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.artifact.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.sm.states.JenkinsState.JenkinsExecutionResponse.Builder.aJenkinsExecutionResponse;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableMap;

import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.CurrentThreadExecutor;
import software.wings.beans.Activity;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.JenkinsState.FilePathAssertionEntry;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

/**
 * Created by peeyushaggarwal on 10/25/16.
 */
public class JenkinsStateTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private JenkinsFactory jenkinsFactory;
  @Mock private SettingsService settingsService;
  @Mock private ExecutorService executorService;
  @Mock private ActivityService activityService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private ExecutionContext executionContext;
  @Mock private Jenkins jenkins;
  @Mock private Build build;
  @Mock private BuildWithDetails buildWithDetails;

  @InjectMocks private JenkinsState jenkinsState = new JenkinsState("jenkins");

  @Before
  public void setUp() throws Exception {
    on(jenkinsState).set("executorService", new CurrentThreadExecutor());
    jenkinsState.setJenkinsConfigId(SETTING_ID);
    jenkinsState.setJobName("testjob");
    when(executionContext.getApp()).thenReturn(anApplication().withUuid(APP_ID).build());
    when(executionContext.getEnv()).thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(activityService.save(any(Activity.class))).thenReturn(anActivity().withUuid(ACTIVITY_ID).build());
    when(settingsService.get(GLOBAL_APP_ID, SETTING_ID))
        .thenReturn(aSettingAttribute()
                        .withValue(aJenkinsConfig()
                                       .withJenkinsUrl("http://jenkins")
                                       .withUsername("username")
                                       .withPassword("password")
                                       .build())
                        .build());
    when(jenkinsFactory.create(anyString(), anyString(), anyString())).thenReturn(jenkins);
    when(jenkins.getBuild(any(QueueReference.class))).thenReturn(build);
    when(build.details()).thenReturn(buildWithDetails);
    when(buildWithDetails.isBuilding()).thenReturn(false);
    when(executionContext.renderExpression(anyString()))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, String.class));
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public void shouldExecuteSuccessfullyWhenBuildPasses() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    jenkinsState.execute(executionContext);
    verify(jenkinsFactory).create("http://jenkins", "username", "password");
    verify(jenkins).trigger("testjob", Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
  }

  @Test
  public void shouldFailWhenBuildFails() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.FAILURE);
    jenkinsState.execute(executionContext);
    verify(jenkinsFactory).create("http://jenkins", "username", "password");
    verify(jenkins).trigger("testjob", Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
  }

  @Test
  public void shouldAssertArtifacts() throws Exception {
    jenkinsState.setFilePathsForAssertion(
        asList(new FilePathAssertionEntry("pom.xml", "${fileData}==\"OK\"", (FilePathAssertionEntry.Status) null)));
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    jenkinsState.execute(executionContext);
    verify(jenkinsFactory).create("http://jenkins", "username", "password");
    verify(jenkins).trigger("testjob", Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
  }

  @Test
  public void shouldHandleAsyncResponse() throws Exception {
    when(executionContext.getStateExecutionData()).thenReturn(aJenkinsExecutionData().build());
    jenkinsState.handleAsyncResponse(executionContext,
        ImmutableMap.of("data",
            aJenkinsExecutionResponse()
                .withActivityId(ACTIVITY_ID)
                .withErrorMessage("Err")
                .withExecutionStatus(ExecutionStatus.FAILED)
                .withJenkinsResult("SUCCESS")
                .withJobUrl("http://jenkins")
                .withFilePathAssertionMap(Collections.emptyList())
                .build()));

    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }
}
