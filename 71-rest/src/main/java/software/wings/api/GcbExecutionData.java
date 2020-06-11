package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.valueOf;
import static software.wings.api.ExecutionDataValue.executionDataValue;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by vglijin on 5/29/20.
 */
@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GcbExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String buildUrl;
  private Map<String, String> jobParameters;
  private Map<String, String> envVars;
  private String activityId;
  private Map<String, String> metadata;
  private String buildId;
  private String description;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return setExecutionData(super.getExecutionSummary());
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return setExecutionData(super.getExecutionDetails());
  }

  private Map<String, ExecutionDataValue> setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    if (isNotEmpty(jobParameters)) {
      executionDetails.put("jobParameters", executionDataValue("Job Parameters", removeNullValues(jobParameters)));
    }

    if (isNotEmpty(envVars)) {
      executionDetails.put("envVars", executionDataValue("Environment Variables", removeNullValues(envVars)));
    }

    executionDetails.put("buildNumber", executionDataValue("Build Number", buildId));
    executionDetails.put("description", executionDataValue("Description", description));
    executionDetails.put("build", executionDataValue("Build Url", buildUrl));

    if (isNotEmpty(metadata)) {
      executionDetails.put("metadata", executionDataValue("Meta-Data", valueOf(removeNullValues(metadata))));
    }
    executionDetails.put("activityId", executionDataValue("Activity Id", activityId));
    return executionDetails;
  }
}
