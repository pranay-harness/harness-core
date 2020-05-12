package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.EntityType;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by sgurubelli on 11/20/17.
 */
@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ArtifactCollectionExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
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
  private EntityType entityType;
  private String entityId;
  private String serviceId;
  private String artifactVariableName;

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
