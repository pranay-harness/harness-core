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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.wings.beans.command.GcbTaskParams;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildStatus;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.GcbState;

import java.util.List;
import java.util.Map;

/**
 * Created by vglijin on 5/29/20.
 */
@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GcbExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  public static final String GCB_URL = "https://console.cloud.google.com/cloud-build/builds/";
  @NotNull private String activityId;
  @Nullable private String buildUrl;
  @Nullable private Map<String, String> jobParameters;
  @Nullable private Map<String, String> metadata;
  @Nullable private String buildId;
  @Nullable private List<String> tags;
  @Nullable private GcbBuildStatus buildStatus;
  @Nullable private String name;
  @Nullable private String createTime;
  @Nullable private Map<String, String> substitutions;
  @Nullable private String logUrl;

  @NotNull
  public GcbExecutionData withDelegateResponse(@NotNull final GcbState.GcbDelegateResponse delegateResponse) {
    GcbTaskParams params = delegateResponse.getParams();
    name = params.getBuildName();
    buildId = params.getBuildId();
    buildUrl = GCB_URL + buildId;
    GcbBuildDetails buildDetails = delegateResponse.getBuild();
    tags = buildDetails.getTags();
    buildStatus = buildDetails.getStatus();
    createTime = buildDetails.getCreateTime();
    substitutions = buildDetails.getSubstitutions();
    logUrl = buildDetails.getLogUrl();
    return this;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return setExecutionData(super.getExecutionSummary());
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return setExecutionData(super.getExecutionDetails());
  }

  private Map<String, ExecutionDataValue> setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    if (isNotEmpty(tags)) {
      executionDetails.put("tags", executionDataValue("Tags", tags));
    }

    if (buildStatus != null) {
      executionDetails.put("status", executionDataValue("Status", buildStatus));
    }

    if (isNotEmpty(name)) {
      executionDetails.put("name", executionDataValue("Name", name));
    }

    if (isNotEmpty(createTime)) {
      executionDetails.put("createTime", executionDataValue("Created Time", createTime));
    }

    if (isNotEmpty(logUrl)) {
      executionDetails.put("logUrl", executionDataValue("Logs Url", logUrl));
    }

    if (isNotEmpty(jobParameters)) {
      executionDetails.put("jobParameters", executionDataValue("Job Parameters", removeNullValues(jobParameters)));
    }

    if (isNotEmpty(substitutions)) {
      executionDetails.put("substitutions", executionDataValue("Substitutions", removeNullValues(substitutions)));
    }

    if (isNotEmpty(buildId)) {
      executionDetails.put("buildNumber", executionDataValue("Build Number", buildId));
    }

    if (isNotEmpty(buildUrl)) {
      executionDetails.put("build", executionDataValue("Build Url", buildUrl));
    }

    if (isNotEmpty(metadata)) {
      executionDetails.put("metadata", executionDataValue("Meta-Data", valueOf(removeNullValues(metadata))));
    }

    if (isNotEmpty(activityId)) {
      putNotNull(executionDetails, "activityId", executionDataValue("Activity Id", activityId));
    }

    return executionDetails;
  }
}
