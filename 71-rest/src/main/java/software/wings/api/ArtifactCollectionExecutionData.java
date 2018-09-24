package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
  private String message;
  private String timeout;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    return setExecutionData(executionDetails);
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    return setExecutionData(executionDetails);
  }

  private Map<String, ExecutionDataValue> setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(
        executionDetails, "timeout", ExecutionDataValue.builder().displayName("Timeout (ms)").value(timeout).build());
    putNotNull(executionDetails, "artifactSource",
        ExecutionDataValue.builder().displayName("Artifact Source").value(artifactSource).build());
    putNotNull(
        executionDetails, "status", ExecutionDataValue.builder().displayName("Status").value(artifactStatus).build());
    putNotNull(
        executionDetails, "jobName", ExecutionDataValue.builder().displayName("Job Name").value(jobName).build());
    putNotNull(
        executionDetails, "buildNo", ExecutionDataValue.builder().displayName("Build / Tag").value(buildNo).build());
    putNotNull(
        executionDetails, "revision", ExecutionDataValue.builder().displayName("Revision").value(revision).build());

    if (isNotEmpty(metadata)) {
      putNotNull(executionDetails, "metadata",
          ExecutionDataValue.builder()
              .displayName("Meta-Data")
              .value(String.valueOf(removeNullValues(metadata)))
              .build());
    }
    putNotNull(executionDetails, "message", ExecutionDataValue.builder().displayName("Message").value(message).build());
    return executionDetails;
  }
}
