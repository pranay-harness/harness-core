package software.wings.delegatetasks;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import software.wings.beans.TaskType;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;

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

  private String jenkinsUrl = "http://jenkins";
  private String userName = "user1";
  private String password = "pass1";
  private String jobName = "job1";
  private Map<String, String> parameters = new HashMap<>();
  private Map<String, String> assertions = new HashMap<>();

  @InjectMocks
  private JenkinsTask jenkinsTask =
      (JenkinsTask) TaskType.JENKINS.getDelegateRunnableTask("id1", new Object[] {}, notifyResponseData -> {});

  @Before
  public void setUp() throws Exception {
    when(jenkinsFactory.create(anyString(), anyString(), anyString())).thenReturn(jenkins);
    when(jenkins.getBuild(any(QueueReference.class))).thenReturn(build);
    when(build.details()).thenReturn(buildWithDetails);
    when(buildWithDetails.isBuilding()).thenReturn(false);
  }

  @Test
  public void shouldExecuteSuccessfullyWhenBuildPasses() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    jenkinsTask.run(jenkinsUrl, userName, password, jobName, parameters, assertions);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
  }

  @Test
  public void shouldFailWhenBuildFails() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.FAILURE);
    jenkinsTask.run(jenkinsUrl, userName, password, jobName, parameters, assertions);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
  }

  @Test
  public void shouldAssertArtifacts() throws Exception {
    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
    jenkinsTask.run(jenkinsUrl, userName, password, jobName, parameters, assertions);
    verify(jenkinsFactory).create(jenkinsUrl, userName, password);
    verify(jenkins).trigger(jobName, Collections.emptyMap());
    verify(jenkins).getBuild(any(QueueReference.class));
  }
}
