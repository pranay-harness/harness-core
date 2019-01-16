package software.wings.api.k8s;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.TaskType;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sStateExecutionData extends StateExecutionData implements ResponseData {
  private String activityId;
  private String releaseName;
  private String namespace;
  private String clusterName;
  private Integer releaseNumber;
  private String commandName;
  private Integer targetInstances;
  private TaskType currentTaskType;
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
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();

    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());
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

    return executionDetails;
  }

  @Override
  public K8sExecutionSummary getStepExecutionSummary() {
    return K8sExecutionSummary.builder()
        .namespace(namespace)
        .releaseName(releaseName)
        .releaseNumber(releaseNumber)
        .targetInstances(targetInstances)
        .build();
  }
}
