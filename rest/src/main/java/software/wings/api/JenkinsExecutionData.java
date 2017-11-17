package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import lombok.Builder;
import lombok.Data;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.FilePathAssertionEntry;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 10/24/16.
 */
@Data
@Builder
public class JenkinsExecutionData extends StateExecutionData implements NotifyResponseData {
  private String jobName;
  private String jobStatus;
  private String buildUrl;
  private List<FilePathAssertionEntry> filePathAssertionMap;
  private Map<String, String> jobParameters;
  private String activityId;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(
        executionDetails, "jobName", anExecutionDataValue().withValue(jobName).withDisplayName("Job Name").build());
    putNotNull(executionDetails, "build", anExecutionDataValue().withValue(buildUrl).withDisplayName("Build").build());
    putNotNull(executionDetails, "jobParameters",
        anExecutionDataValue().withValue(jobParameters).withDisplayName("Job Parameters").build());
    putNotNull(executionDetails, "jobStatus",
        anExecutionDataValue().withValue(jobStatus).withDisplayName("Job Status").build());
    putNotNull(executionDetails, "fileAssertionData",
        anExecutionDataValue().withValue(filePathAssertionMap).withDisplayName("Assertion Data").build());
    putNotNull(executionDetails, "activityId",
        anExecutionDataValue().withValue(activityId).withDisplayName("Activity Id").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, "jobName", anExecutionDataValue().withValue(jobName).withDisplayName("Job Name").build());
    putNotNull(executionDetails, "build", anExecutionDataValue().withValue(buildUrl).withDisplayName("Build").build());
    putNotNull(executionDetails, "jobParameters",
        anExecutionDataValue().withValue(jobParameters).withDisplayName("Job Parameters").build());
    putNotNull(executionDetails, "jobStatus",
        anExecutionDataValue().withValue(jobStatus).withDisplayName("Job Status").build());
    putNotNull(executionDetails, "fileAssertionData",
        anExecutionDataValue().withValue(filePathAssertionMap).withDisplayName("Assertion Data").build());
    putNotNull(executionDetails, "activityId",
        anExecutionDataValue().withValue(activityId).withDisplayName("Activity Id").build());
    return executionDetails;
  }
}
