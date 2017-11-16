package software.wings.delegatetasks;

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

  private JenkinsConfig jenkinsConfig;
  private String jenkinsUrl = "http://jenkins";
  private String userName = "user1";
  private char[] password = "pass1".toCharArray();
  private String jobName = "job1";
  private String activityId = "activityId";
  private String stateName = "jenkins_state";
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
    jenkinsConfig = JenkinsConfig.builder().jenkinsUrl(jenkinsUrl).username(userName).password(password).build();
  }

  @Test
  public void shouldExecuteSuccessfullyWhenBuildPasses() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    jenkinsTask.run(jenkinsConfig, Collections.emptyList(), jobName, parameters, assertions, activityId, stateName);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
  }

  @Test
  public void shouldFailWhenBuildFails() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.FAILURE);
    jenkinsTask.run(jenkinsConfig, Collections.emptyList(), jobName, parameters, assertions, activityId, stateName);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
  }

  @Test
  public void shouldAssertArtifacts() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    jenkinsTask.run(jenkinsConfig, Collections.emptyList(), jobName, parameters, assertions, activityId, stateName);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
  }
}
