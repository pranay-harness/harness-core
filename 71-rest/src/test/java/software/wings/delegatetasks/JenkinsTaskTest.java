package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Log.Builder.aLog;

import com.google.inject.Inject;

import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.exception.GeneralException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.WingsBaseTest;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsSubTaskType;
import software.wings.beans.Log;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.TaskType;
import software.wings.beans.command.JenkinsTaskParams;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.impl.jenkins.JenkinsUtils;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.JenkinsState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rishi on 12/16/16.
 */
public class JenkinsTaskTest extends WingsBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private JenkinsFactory jenkinsFactory;
  @Mock private Jenkins jenkins;
  @Mock private Build build;
  @Mock private BuildWithDetails buildWithDetails;
  @Mock private EncryptionService encryptionService;
  @Mock private DelegateLogService logService;
  @Inject @InjectMocks JenkinsUtils jenkinsUtil;
  @Captor private ArgumentCaptor<Log> logsCaptor;
  @Captor private ArgumentCaptor<String> activityCaptor;

  private String jenkinsUrl = "http://jenkins";
  private String userName = "user1";
  private char[] password = "pass1".toCharArray();
  private String jobName = "job1";
  private String activityId = "activityId";
  private String stateName = "jenkins_state";
  private String appId = "testAppId";
  private JenkinsConfig jenkinsConfig =
      JenkinsConfig.builder().jenkinsUrl(jenkinsUrl).username(userName).password(password).build();
  private Map<String, String> parameters = new HashMap<>();
  private Map<String, String> assertions = new HashMap<>();

  @InjectMocks
  private JenkinsTask jenkinsTask = (JenkinsTask) TaskType.JENKINS.getDelegateRunnableTask("delid1",
      DelegateTask.builder().data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(jenkinsTask).set("jenkinsUtil", jenkinsUtil);
    when(jenkinsFactory.create(anyString(), anyString(), any(char[].class))).thenReturn(jenkins);
    when(jenkins.getBuild(any(QueueReference.class))).thenReturn(build);
    when(build.details()).thenReturn(buildWithDetails);
    when(buildWithDetails.isBuilding()).thenReturn(false);
    when(buildWithDetails.getConsoleOutputText()).thenReturn("console output");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldExecuteSuccessfullyWhenBuildPasses() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    when(buildWithDetails.getDescription()).thenReturn("test-description");

    JenkinsTaskParams params = buildJenkinsTaskParams();
    params.setSubTaskType(JenkinsSubTaskType.START_TASK);
    params.setQueuedBuildUrl(jenkinsUrl);

    // Invoke Jenkins Start Task
    when(jenkins.trigger(jobName, Collections.emptyMap())).thenReturn(new QueueReference(jenkinsUrl));
    JenkinsState.JenkinsExecutionResponse response = jenkinsTask.run(params);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    // Invoke Jenkins Poll Task
    params.setSubTaskType(JenkinsSubTaskType.POLL_TASK);
    response = jenkinsTask.run(params);
    assertThat(response.getDescription()).isEqualTo("test-description");
    assertThat(response.getEnvVars()).isNullOrEmpty();
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailWhenBuildFails() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.FAILURE);

    JenkinsTaskParams params = buildJenkinsTaskParams();
    params.setSubTaskType(JenkinsSubTaskType.START_TASK);
    params.setQueuedBuildUrl(jenkinsUrl);

    // Jenkins Start Task
    when(jenkins.trigger(jobName, Collections.emptyMap())).thenReturn(new QueueReference(jenkinsUrl));
    JenkinsState.JenkinsExecutionResponse response = jenkinsTask.run(params);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    // Jenkins Poll Task
    params.setSubTaskType(JenkinsSubTaskType.POLL_TASK);
    response = jenkinsTask.run(params);
    assertThat(response.getEnvVars()).isNullOrEmpty();
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailWhenBuildUnstable() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.UNSTABLE);

    JenkinsTaskParams params = buildJenkinsTaskParams();
    params.setSubTaskType(JenkinsSubTaskType.START_TASK);
    params.setQueuedBuildUrl(jenkinsUrl);

    // Jenkins Start Task
    when(jenkins.trigger(jobName, Collections.emptyMap())).thenReturn(new QueueReference(jenkinsUrl));
    JenkinsState.JenkinsExecutionResponse response = jenkinsTask.run(params);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    // Jenkins Poll Task
    params.setSubTaskType(JenkinsSubTaskType.POLL_TASK);
    response = jenkinsTask.run(params);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFailWhenNoJobFound() throws Exception {
    when(build.details()).thenThrow(new HttpResponseException(404, "Job Not found"));

    JenkinsTaskParams params = buildJenkinsTaskParams();
    params.setSubTaskType(JenkinsSubTaskType.START_TASK);
    params.setQueuedBuildUrl(jenkinsUrl);

    JenkinsState.JenkinsExecutionResponse response = jenkinsTask.run(params);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(response.getErrorMessage()).isNotEmpty();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldPassWhenBuildUnstableAndUnstableSuccessSet() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.UNSTABLE);

    JenkinsTaskParams params = buildJenkinsTaskParams();
    params.setSubTaskType(JenkinsSubTaskType.START_TASK);
    params.setQueuedBuildUrl(jenkinsUrl);
    params.setUnstableSuccess(true);

    when(jenkins.trigger(jobName, Collections.emptyMap())).thenReturn(new QueueReference(jenkinsUrl));
    JenkinsState.JenkinsExecutionResponse response = jenkinsTask.run(params);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    // Jenkins Poll Task
    params.setSubTaskType(JenkinsSubTaskType.POLL_TASK);
    response = jenkinsTask.run(params);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldInjectEnvVarsWhenInjectEnvVarsSet() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    when(buildWithDetails.getDescription()).thenReturn("test-description");

    JenkinsTaskParams params = buildJenkinsTaskParams();
    params.setSubTaskType(JenkinsSubTaskType.START_TASK);
    params.setQueuedBuildUrl(jenkinsUrl);
    params.setInjectEnvVars(true);

    // Invoke Jenkins Start Task
    when(jenkins.trigger(jobName, Collections.emptyMap())).thenReturn(new QueueReference(jenkinsUrl));
    JenkinsState.JenkinsExecutionResponse response = jenkinsTask.run(params);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    assertThat(response.getEnvVars()).isNullOrEmpty();
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    // Invoke Jenkins Poll Task
    Map<String, String> envVars = new HashMap<>();
    String env1 = "ENV1";
    envVars.put(env1, env1 + "_VAL");
    when(jenkins.getEnvVars(anyString())).thenReturn(envVars);
    params.setSubTaskType(JenkinsSubTaskType.POLL_TASK);
    response = jenkinsTask.run(params);
    assertThat(response.getDescription()).isEqualTo("test-description");
    assertThat(response.getEnvVars()).isNotEmpty().containsOnly(entry(env1, envVars.get(env1)));
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFailWhenGetEnvVarsThrows() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    when(buildWithDetails.getDescription()).thenReturn("test-description");

    JenkinsTaskParams params = buildJenkinsTaskParams();
    params.setSubTaskType(JenkinsSubTaskType.START_TASK);
    params.setQueuedBuildUrl(jenkinsUrl);
    params.setInjectEnvVars(true);

    // Invoke Jenkins Start Task
    when(jenkins.trigger(jobName, Collections.emptyMap())).thenReturn(new QueueReference(jenkinsUrl));
    JenkinsState.JenkinsExecutionResponse response = jenkinsTask.run(params);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    assertThat(response.getEnvVars()).isNullOrEmpty();
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    // Invoke Jenkins Poll Task
    Map<String, String> envVars = new HashMap<>();
    String env1 = "ENV1";
    envVars.put(env1, env1 + "_VAL");
    when(jenkins.getEnvVars(anyString())).thenThrow(new WingsException(INVALID_ARTIFACT_SERVER, USER));
    params.setSubTaskType(JenkinsSubTaskType.POLL_TASK);
    response = jenkinsTask.run(params);
    assertThat(response.getErrorMessage()).isNotBlank();
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldFailWhenTriggerJobAPIFails() throws Exception {
    JenkinsTaskParams params = buildJenkinsTaskParams();
    params.setSubTaskType(JenkinsSubTaskType.START_TASK);
    params.setQueuedBuildUrl(jenkinsUrl);

    // Jenkins Start Task
    when(jenkins.trigger(jobName, Collections.emptyMap())).thenThrow(new GeneralException("Exception"));
    JenkinsState.JenkinsExecutionResponse response = jenkinsTask.run(params);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(response.getErrorMessage()).isNotEmpty();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldFailWhenTriggerJobAPIFailsWithUnknownException() throws Exception {
    JenkinsTaskParams params = buildJenkinsTaskParams();
    params.setSubTaskType(JenkinsSubTaskType.START_TASK);
    params.setQueuedBuildUrl(jenkinsUrl);

    // Jenkins Start Task
    when(jenkins.trigger(jobName, Collections.emptyMap())).then(invocationOnMock -> { throw new Exception(); });
    JenkinsState.JenkinsExecutionResponse response = jenkinsTask.run(params);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(response.getErrorMessage()).isNotEmpty();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldPassAppIdForLoggingScopeTroughJenkinsTaskParams() {
    JenkinsTaskParams jenkinsTaskParams = buildJenkinsTaskParams();
    Log log =
        constructLog(jenkinsTaskParams.getActivityId(), jenkinsTaskParams.getUnitName(), jenkinsTaskParams.getAppId(),
            LogLevel.INFO, "Triggering Jenkins Job : " + jenkinsTaskParams.getJobName(), RUNNING);

    logService.save(jenkinsTaskParams.getActivityId(), log);
    verify(logService).save(activityCaptor.capture(), logsCaptor.capture());
    assertThat(logsCaptor.getValue().getAppId()).isEqualTo(appId);
  }

  // Helper functions
  private JenkinsTaskParams buildJenkinsTaskParams() {
    return JenkinsTaskParams.builder()
        .jenkinsConfig(jenkinsConfig)
        .encryptedDataDetails(emptyList())
        .jobName(jobName)
        .parameters(parameters)
        .activityId(activityId)
        .filePathsForAssertion(assertions)
        .unitName(stateName)
        .appId(appId)
        .build();
  }

  private Log constructLog(String activityId, String stateName, String appId, LogLevel logLevel, String logLine,
      CommandExecutionResult.CommandExecutionStatus commandExecutionStatus) {
    return aLog()
        .withActivityId(activityId)
        .withCommandUnitName(stateName)
        .withAppId(appId)
        .withLogLevel(logLevel)
        .withLogLine(logLine)
        .withExecutionResult(commandExecutionStatus)
        .build();
  }
}
