package software.wings.api.k8s;

import static io.harness.annotations.dev.HarnessModule._871_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
@TargetModule(_871_CG_BEANS)
@OwnedBy(CDP)
public class K8sStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String releaseName;
  private String namespace;
  private String clusterName;
  private String cloudProvider;
  private Integer releaseNumber;
  private String commandName;
  private Integer targetInstances;
  private TaskType currentTaskType;
  @Builder.Default private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();
  private String loadBalancer;
  @Builder.Default private Map<K8sValuesLocation, ApplicationManifest> applicationManifestMap = new HashMap<>();
  @Builder.Default private Map<K8sValuesLocation, Collection<String>> valuesFiles = new HashMap<>();
  @Builder.Default private Set<String> namespaces = new HashSet<>();
  private HelmChartInfo helmChartInfo;
  private String blueGreenStageColor;
  private Set<String> delegateSelectors;

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
    putNotNull(executionDetails, "cloudProvider",
        ExecutionDataValue.builder().value(cloudProvider).displayName("Cloud Provider").build());
    putNotNull(executionDetails, "cluster",
        ExecutionDataValue.builder().value(clusterName).displayName("Cluster Name").build());
    putNotNull(
        executionDetails, "namespace", ExecutionDataValue.builder().value(namespace).displayName("Namespace").build());
    putNotNull(executionDetails, "releaseName",
        ExecutionDataValue.builder().value(releaseName).displayName("Release Name").build());
    putNotNull(executionDetails, "releaseNumber",
        ExecutionDataValue.builder().value(releaseNumber).displayName("Release Number").build());
    putNotNull(executionDetails, "targetInstances",
        ExecutionDataValue.builder().value(targetInstances).displayName("Target Instance Count").build());
    putNotNull(executionDetails, "loadBalancer",
        ExecutionDataValue.builder().value(loadBalancer).displayName("Load Balancer").build());
    return executionDetails;
  }

  @Override
  public K8sExecutionSummary getStepExecutionSummary() {
    return K8sExecutionSummary.builder()
        .namespace(namespace)
        .releaseName(releaseName)
        .releaseNumber(releaseNumber)
        .targetInstances(targetInstances)
        .namespaces(namespaces)
        .helmChartInfo(helmChartInfo)
        .blueGreenStageColor(blueGreenStageColor)
        .delegateSelectors(delegateSelectors)
        .build();
  }
}
