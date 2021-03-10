package software.wings.api;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.beans.TaskType;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.sm.StateExecutionData;

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
public class ARMStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private TaskType taskType;
  private String activityId;
  private GitFetchFilesFromMultipleRepoResult fetchFilesResult;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId", ExecutionDataValue.builder().displayName("").value(activityId).build());
    return executionDetails;
  }
}
