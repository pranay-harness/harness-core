package software.wings.api;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.beans.TaskType;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.sm.StateExecutionData;

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

  private String inlineTemplateForRollback;
  private String inlineVariablesForRollback;
}
