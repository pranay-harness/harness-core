package software.wings.api;

import com.google.common.collect.Maps;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
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

  @Builder.Default private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = Maps.newLinkedHashMap();

    if (getDelegateMetaInfo() != null) {
      putNotNull(executionDetails, "delegateName",
          ExecutionDataValue.builder().displayName("Delegate").value(this.getDelegateMetaInfo().getHostName()).build());
    }

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
    putNotNull(
        executionDetails, "namespace", ExecutionDataValue.builder().value(namespace).displayName("Namespace").build());
    putNotNull(executionDetails, "rollbackVersion",
        ExecutionDataValue.builder().value(rollbackVersion).displayName("Release rollback Version").build());
    putNotNull(executionDetails, "commandFlags",
        ExecutionDataValue.builder().value(commandFlags).displayName("Command flags").build());

    return executionDetails;
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
