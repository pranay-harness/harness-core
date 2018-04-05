package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.FilePathAssertionEntry;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 10/24/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class JenkinsExecutionData extends StateExecutionData implements NotifyResponseData {
  private String jobName;
  private String jobStatus;
  private String buildUrl;
  private List<FilePathAssertionEntry> filePathAssertionMap;
  private Map<String, String> jobParameters;
  private String activityId;
  private Map<String, String> metadata;
  private String buildNumber;
  private String description;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    setExecutionData(executionDetails);
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    setExecutionData(executionDetails);
    return executionDetails;
  }

  private void setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(
        executionDetails, "jobName", ExecutionDataValue.builder().displayName("Job Name").value(jobName).build());

    if (jobParameters != null) {
      putNotNull(executionDetails, "jobParameters",
          ExecutionDataValue.builder().displayName("Job Parameters").value(removeNullValues(jobParameters)).build());
    }

    putNotNull(executionDetails, "fileAssertionData",
        ExecutionDataValue.builder().displayName("Assertion Data").value(filePathAssertionMap).build());
    putNotNull(
        executionDetails, "jobStatus", ExecutionDataValue.builder().displayName("Job Status").value(jobStatus).build());
    putNotNull(executionDetails, "buildNumber",
        ExecutionDataValue.builder().displayName("Build Number").value(buildNumber).build());
    putNotNull(executionDetails, "description",
        ExecutionDataValue.builder().displayName("Description").value(description).build());
    putNotNull(
        executionDetails, "build", ExecutionDataValue.builder().displayName("Build Url").value(buildUrl).build());
    if (metadata != null) {
      putNotNull(executionDetails, "metadata",
          ExecutionDataValue.builder().displayName("Meta-Data").value(removeNullValues(jobParameters)).build());
    }
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().displayName("Activity Id").value(activityId).build());
  }
}
