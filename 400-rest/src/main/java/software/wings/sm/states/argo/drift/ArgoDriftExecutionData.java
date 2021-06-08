package software.wings.sm.states.argo.drift;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.api.ExecutionDataValue;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.argo.ArgoStepExecutionSummary;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ArgoDriftExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String envId;
  private io.harness.beans.EnvironmentType environmentType;
  private String appId;
  private String infraMappingId;
  private String commandName;
  private boolean rollback;

  private String argoServerUrl;
  private String clusterUrl;
  private String argoAppName;
  private String repoUrl;
  private String repoRef;
  private String syncOption;
  private String argoConnectorId;
  // private String driftResponse;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());
    putNotNull(executionDetails, "appId", ExecutionDataValue.builder().value(appId).displayName("App Id").build());
    putNotNull(executionDetails, "envId", ExecutionDataValue.builder().value(envId).displayName("Env Id").build());
    putNotNull(executionDetails, "infraMappingId",
        ExecutionDataValue.builder().value(infraMappingId).displayName("Infra MappingId Id").build());
    putNotNull(executionDetails, "argoServerUrl",
        ExecutionDataValue.builder().value(argoServerUrl).displayName("Argo Server Url").build());
    putNotNull(executionDetails, "clusterUrl",
        ExecutionDataValue.builder().value(clusterUrl).displayName("K8 Cluster Url").build());
    putNotNull(executionDetails, "argoAppName",
        ExecutionDataValue.builder().value(argoAppName).displayName("Argo Application Name").build());
    putNotNull(
        executionDetails, "repoUrl", ExecutionDataValue.builder().value(repoUrl).displayName("Git Repo").build());
    putNotNull(
        executionDetails, "repoRef", ExecutionDataValue.builder().value(repoRef).displayName("Repo Ref").build());
    putNotNull(executionDetails, "syncOption",
        ExecutionDataValue.builder().value(syncOption).displayName("Sync Option").build());
    putNotNull(executionDetails, "argoConnectorId",
        ExecutionDataValue.builder().value(argoConnectorId).displayName("Argo Connector Id").build());
    return executionDetails;
  }

  @Override
  public ArgoStepExecutionSummary getStepExecutionSummary() {
    return ArgoStepExecutionSummary.builder()
        .argoAppName(argoAppName)
        .argoServerUrl(argoServerUrl)
        .repoName(repoUrl)
        .repoRef(repoRef)
        .syncOption(syncOption)
        .build();
  }
}