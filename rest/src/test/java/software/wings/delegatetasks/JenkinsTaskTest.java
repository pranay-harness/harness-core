package software.wings.delegatetasks;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;

import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.WingsBaseTest;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsSubTaskType;
import software.wings.beans.TaskType;
import software.wings.beans.command.JenkinsTaskParams;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.impl.jenkins.JenkinsUtil;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.ExecutionStatus;
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
  @Inject @InjectMocks JenkinsUtil jenkinsUtil;

  private String jenkinsUrl = "http://jenkins";
  private String userName = "user1";
  private char[] password = "pass1".toCharArray();
  private String jobName = "job1";
  private String activityId = "activityId";
  private String stateName = "jenkins_state";
  private JenkinsConfig jenkinsConfig =
      JenkinsConfig.builder().jenkinsUrl(jenkinsUrl).username(userName).password(password).build();
  private Map<String, String> parameters = new HashMap<>();
  private Map<String, String> assertions = new HashMap<>();

  @InjectMocks
  private JenkinsTask jenkinsTask = (JenkinsTask) TaskType.JENKINS.getDelegateRunnableTask(
      "delid1", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

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
    assertEquals("jenkins job description didn't come through", "test-description", response.getDescription());
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
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
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
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
        .build();
  }
}
