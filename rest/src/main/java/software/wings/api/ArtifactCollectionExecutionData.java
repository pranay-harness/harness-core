package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by sgurubelli on 11/20/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class ArtifactCollectionExecutionData extends StateExecutionData implements NotifyResponseData {
  private String artifactSource;
  private String artifactStatus;
  private String jobName;
  private String buildUrl;
  private String buildNo;
  private String revision;
  private Map<String, String> metadata;
  private String artifactStreamId;
  private String artifactId;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "artifactSource",
        anExecutionDataValue().withValue(artifactSource).withDisplayName("Artifact Source").build());
    putNotNull(
        executionDetails, "status", anExecutionDataValue().withValue(artifactStatus).withDisplayName("Status").build());
    putNotNull(
        executionDetails, "jobName", anExecutionDataValue().withValue(jobName).withDisplayName("Job Name").build());
    putNotNull(
        executionDetails, "buildNo", anExecutionDataValue().withValue(buildNo).withDisplayName("Build / Tag").build());
    putNotNull(
        executionDetails, "revision", anExecutionDataValue().withValue(revision).withDisplayName("Revision").build());
    if (metadata != null) {
      putNotNull(executionDetails, "metadata",
          anExecutionDataValue().withValue(String.valueOf(metadata)).withDisplayName("Meta-Data").build());
    }
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "artifactSource",
        anExecutionDataValue().withValue(artifactSource).withDisplayName("Artifact Source").build());
    putNotNull(
        executionDetails, "status", anExecutionDataValue().withValue(artifactStatus).withDisplayName("Status").build());
    putNotNull(
        executionDetails, "jobName", anExecutionDataValue().withValue(jobName).withDisplayName("Job Name").build());
    putNotNull(
        executionDetails, "buildNo", anExecutionDataValue().withValue(buildNo).withDisplayName("Build / Tag").build());
    putNotNull(
        executionDetails, "revision", anExecutionDataValue().withValue(revision).withDisplayName("Revision").build());
    if (metadata != null) {
      putNotNull(executionDetails, "metadata",
          anExecutionDataValue().withValue(String.valueOf(metadata)).withDisplayName("Meta-Data").build());
    }
    return executionDetails;
  }
}
