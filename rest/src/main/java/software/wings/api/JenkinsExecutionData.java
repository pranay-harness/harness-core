package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

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
        executionDetails, "jobName", anExecutionDataValue().withValue(jobName).withDisplayName("Job Name").build());

    if (jobParameters != null) {
      putNotNull(executionDetails, "jobParameters",
          anExecutionDataValue().withValue(removeNullValues(jobParameters)).withDisplayName("Job Parameters").build());
    }

    putNotNull(executionDetails, "fileAssertionData",
        anExecutionDataValue().withValue(filePathAssertionMap).withDisplayName("Assertion Data").build());
    putNotNull(executionDetails, "jobStatus",
        anExecutionDataValue().withValue(jobStatus).withDisplayName("Job Status").build());
    putNotNull(executionDetails, "buildNumber",
        anExecutionDataValue().withValue(buildNumber).withDisplayName("Build Number").build());
    putNotNull(executionDetails, "description",
        anExecutionDataValue().withValue(description).withDisplayName("Description").build());
    putNotNull(
        executionDetails, "build", anExecutionDataValue().withValue(buildUrl).withDisplayName("Build Url").build());
    if (metadata != null) {
      putNotNull(executionDetails, "metadata",
          anExecutionDataValue().withValue(removeNullValues(jobParameters)).withDisplayName("Meta-Data").build());
    }
    putNotNull(executionDetails, "activityId",
        anExecutionDataValue().withValue(activityId).withDisplayName("Activity Id").build());
  }
}
