package software.wings.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 3/30/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelmDeployStateExecutionData extends StateExecutionData implements NotifyResponseData {
  private String activityId;
  private String commandName;
  private String chartRepositoryUrl;
  private String chartName;
  private String chartVersion;
  private String releaseName;
  private Integer releaseOldVersion;
  private Integer releaseNewVersion;
  private Integer rollbackVersion;
  private boolean rollback;
  @Builder.Default private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    setInternalExecutionDetails(executionDetails);

    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    setInternalExecutionDetails(executionDetails);

    return executionDetails;
  }

  private Map<String, ExecutionDataValue> setInternalExecutionDetails(
      Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());
    putNotNull(executionDetails, "chartRepositoryUrl",
        ExecutionDataValue.builder().value(chartRepositoryUrl).displayName("Chart Repository").build());
    putNotNull(
        executionDetails, "chartName", ExecutionDataValue.builder().value(chartName).displayName("Chart Name").build());
    putNotNull(executionDetails, "chartVersion",
        ExecutionDataValue.builder().value(chartVersion).displayName("Chart Version").build());
    putNotNull(executionDetails, "releaseName",
        ExecutionDataValue.builder().value(releaseName).displayName("Release Name").build());
    putNotNull(executionDetails, "releaseOldVersion",
        ExecutionDataValue.builder().value(releaseOldVersion).displayName("Release Old Version").build());
    putNotNull(executionDetails, "releaseNewVersion",
        ExecutionDataValue.builder().value(releaseNewVersion).displayName("Release New Version").build());
    putNotNull(executionDetails, "rollbackVersion",
        ExecutionDataValue.builder().value(rollbackVersion).displayName("Release rollback Version").build());
    return executionDetails;
  }

  @Override
  public HelmSetupExecutionSummary getStepExecutionSummary() {
    return HelmSetupExecutionSummary.builder()
        .releaseName(releaseName)
        .prevVersion(releaseOldVersion)
        .newVersion(releaseNewVersion)
        .build();
  }
}
