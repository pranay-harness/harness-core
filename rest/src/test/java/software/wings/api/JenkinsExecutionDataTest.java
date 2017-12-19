package software.wings.api;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import software.wings.sm.ExecutionStatus;

/**
 * Created by peeyushaggarwal on 10/25/16.
 */
public class JenkinsExecutionDataTest {
  private JenkinsExecutionData jenkinsExecutionData =
      JenkinsExecutionData.builder().jobName("testjob").buildUrl("http://jenkins/testjob/11").build();

  @Before
  public void setup() {
    jenkinsExecutionData.setErrorMsg("Err");
    jenkinsExecutionData.setJobStatus("ERROR");
    jenkinsExecutionData.setStatus(ExecutionStatus.FAILED);
  }

  @Test
  public void shouldGetExecutionSummary() throws Exception {
    assertThat(jenkinsExecutionData.getExecutionSummary())
        .containsAllEntriesOf(ImmutableMap.of("jobName",
            anExecutionDataValue().withValue("testjob").withDisplayName("Job Name").build(), "build",
            anExecutionDataValue().withValue("http://jenkins/testjob/11").withDisplayName("Build Url").build(),
            "jobStatus", anExecutionDataValue().withValue("ERROR").withDisplayName("Job Status").build()));
  }

  @Test
  public void shouldGetExecutionDetails() throws Exception {
    assertThat(jenkinsExecutionData.getExecutionDetails())
        .containsAllEntriesOf(ImmutableMap.of("jobName",
            anExecutionDataValue().withValue("testjob").withDisplayName("Job Name").build(), "build",
            anExecutionDataValue().withValue("http://jenkins/testjob/11").withDisplayName("Build Url").build(),
            "jobStatus", anExecutionDataValue().withValue("ERROR").withDisplayName("Job Status").build()));
  }
}
