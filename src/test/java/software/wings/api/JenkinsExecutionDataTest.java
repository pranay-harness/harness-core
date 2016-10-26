package software.wings.api;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;
import static software.wings.api.JenkinsExecutionData.Builder.aJenkinsExecutionData;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import software.wings.sm.ExecutionStatus;

/**
 * Created by peeyushaggarwal on 10/25/16.
 */
public class JenkinsExecutionDataTest {
  private JenkinsExecutionData jenkinsExecutionData = aJenkinsExecutionData()
                                                          .withJobName("testjob")
                                                          .withBuildUrl("http://jenkins/testjob/11")
                                                          .withErrorMsg("Err")
                                                          .withJobStatus("ERROR")
                                                          .withStatus(ExecutionStatus.FAILED)
                                                          .build();

  @Test
  public void shouldGetExecutionSummary() throws Exception {
    assertThat(jenkinsExecutionData.getExecutionSummary())
        .containsAllEntriesOf(
            ImmutableMap.of("jobName", anExecutionDataValue().withValue("testjob").withDisplayName("Job Name").build(),
                "build", anExecutionDataValue().withValue("http://jenkins/testjob/11").withDisplayName("Build").build(),
                "jobStatus", anExecutionDataValue().withValue("ERROR").withDisplayName("Job Status").build()));
  }

  @Test
  public void shouldGetExecutionDetails() throws Exception {
    assertThat(jenkinsExecutionData.getExecutionDetails())
        .containsAllEntriesOf(
            ImmutableMap.of("jobName", anExecutionDataValue().withValue("testjob").withDisplayName("Job Name").build(),
                "build", anExecutionDataValue().withValue("http://jenkins/testjob/11").withDisplayName("Build").build(),
                "jobStatus", anExecutionDataValue().withValue("ERROR").withDisplayName("Job Status").build()));
  }
}
