package software.wings.api;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 3/30/18.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class HelmDeployStateExecutionData extends StateExecutionData implements ResponseData {
  private String activityId;
  private String commandName;
  private String chartRepositoryUrl;
  private String chartName;
  private String chartVersion;
  private String releaseName;
  private Integer releaseOldVersion;
  private Integer releaseNewVersion;
  private Integer rollbackVersion;
  private String namespace;
  private boolean rollback;
  private String commandFlags;
  private TaskType currentTaskType;
  @Builder.Default private Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
  @Builder.Default private Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();

  @Builder.Default private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    setExecutionData(executionDetails);
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionSummary = super.getExecutionSummary();
    setExecutionData(executionSummary);
    return executionSummary;
  }

  private void setExecutionData(Map<String, ExecutionDataValue> executionData) {
    String errorMsg = getErrorMsg();
    if (isNotBlank(errorMsg) && errorMsg.length() > 256) {
      putNotNull(executionData, "errorMsg",
          ExecutionDataValue.builder().displayName("Message").value(errorMsg.substring(0, 256)).build());
    }

    putNotNull(
        executionData, "activityId", ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());
    putNotNull(executionData, "chartRepositoryUrl",
        ExecutionDataValue.builder().value(chartRepositoryUrl).displayName("Chart Repository").build());
    putNotNull(
        executionData, "chartName", ExecutionDataValue.builder().value(chartName).displayName("Chart Name").build());
    putNotNull(executionData, "chartVersion",
        ExecutionDataValue.builder().value(chartVersion).displayName("Chart Version").build());
    putNotNull(executionData, "releaseName",
        ExecutionDataValue.builder().value(releaseName).displayName("Release Name").build());
    putNotNull(executionData, "releaseOldVersion",
        ExecutionDataValue.builder().value(releaseOldVersion).displayName("Release Old Version").build());
    putNotNull(executionData, "releaseNewVersion",
        ExecutionDataValue.builder().value(releaseNewVersion).displayName("Release New Version").build());
    putNotNull(
        executionData, "namespace", ExecutionDataValue.builder().value(namespace).displayName("Namespace").build());
    putNotNull(executionData, "rollbackVersion",
        ExecutionDataValue.builder().value(rollbackVersion).displayName("Release rollback Version").build());
    putNotNull(executionData, "commandFlags",
        ExecutionDataValue.builder().value(commandFlags).displayName("Command flags").build());
  }

  @Override
  public HelmSetupExecutionSummary getStepExecutionSummary() {
    return HelmSetupExecutionSummary.builder()
        .releaseName(releaseName)
        .prevVersion(releaseOldVersion)
        .newVersion(releaseNewVersion)
        .rollbackVersion(rollbackVersion)
        .namespace(namespace)
        .commandFlags(commandFlags)
        .build();
  }
}
