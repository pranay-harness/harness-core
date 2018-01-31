package software.wings.delegatetasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.TaskType;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.JenkinsState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rishi on 12/16/16.
 */
public class JenkinsTaskTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private JenkinsFactory jenkinsFactory;
  @Mock private Jenkins jenkins;
  @Mock private Build build;
  @Mock private BuildWithDetails buildWithDetails;
  @Mock private EncryptionService encryptionService;
  @Mock private DelegateLogService logService;

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
    when(jenkinsFactory.create(anyString(), anyString(), any(char[].class))).thenReturn(jenkins);
    when(jenkins.getBuild(any(QueueReference.class))).thenReturn(build);
    when(build.details()).thenReturn(buildWithDetails);
    when(buildWithDetails.isBuilding()).thenReturn(false);
    when(buildWithDetails.getConsoleOutputText()).thenReturn("console output");
  }

  @Test
  public void shouldExecuteSuccessfullyWhenBuildPasses() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    JenkinsState.JenkinsExecutionResponse response =
        jenkinsTask.run(jenkinsConfig, Collections.emptyList(), jobName, parameters, assertions, activityId, stateName);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  public void shouldFailWhenBuildFails() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.FAILURE);
    JenkinsState.JenkinsExecutionResponse response =
        jenkinsTask.run(jenkinsConfig, Collections.emptyList(), jobName, parameters, assertions, activityId, stateName);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  public void shouldFailWhenBuildUnstable() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.UNSTABLE);
    JenkinsState.JenkinsExecutionResponse response =
        jenkinsTask.run(jenkinsConfig, Collections.emptyList(), jobName, parameters, assertions, activityId, stateName);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }
}
