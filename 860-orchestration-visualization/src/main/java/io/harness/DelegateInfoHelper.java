package io.harness;

import static io.harness.data.structure.HarnessStringUtils.nullIfEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.client.DelegateSelectionLogHttpClient;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.dto.GraphDelegateSelectionLogParams;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.ExecutionModeUtils;
import io.harness.network.SafeHttpCall;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class DelegateInfoHelper {
  // Todo: Remove dependency of orchestrationVisualization from 120-ng-manager
  @Inject(optional = true) private DelegateSelectionLogHttpClient delegateSelectionLogHttpClient;

  public List<GraphDelegateSelectionLogParams> getDelegateInformationForGivenTask(
      List<ExecutableResponse> executableResponses, ExecutionMode executionMode, String accountId) {
    if (!ExecutionModeUtils.isTaskMode(executionMode)) {
      return new ArrayList<>();
    }
    return executableResponses.stream()
        .map(executableResponse -> {
          try {
            TaskInfo taskInfo = TaskInfo.fromExecutableResponse(executableResponse);
            if (taskInfo.getTaskId() != null) {
              DelegateSelectionLogParams resource =
                  SafeHttpCall.execute(delegateSelectionLogHttpClient.getDelegateInfo(accountId, taskInfo.getTaskId()))
                      .getResource();
              return GraphDelegateSelectionLogParams.builder()
                  .selectionLogParams(resource)
                  .taskId(taskInfo.getTaskId())
                  .taskName(taskInfo.getTaskName())
                  .build();
            }
            return null;
          } catch (Exception exception) {
            log.error("Not able to talk to delegate service. Ignoring delegate Information");
            return null;
          }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private static class TaskInfo {
    @Getter private String taskId;
    @Getter private String taskName;

    private TaskInfo() {}

    private TaskInfo(String taskId, String taskName) {
      this.taskId = taskId;
      this.taskName = taskName;
    }

    public static TaskInfo fromExecutableResponse(ExecutableResponse response) {
      switch (response.getResponseCase()) {
        case TASK:
          return new TaskInfo(
              nullIfEmpty(response.getTask().getTaskId()), nullIfEmpty(response.getTask().getTaskName()));
        case TASKCHAIN:
          return new TaskInfo(
              nullIfEmpty(response.getTaskChain().getTaskId()), nullIfEmpty(response.getTaskChain().getTaskName()));
        default:
          throw new InvalidRequestException("Trying to extract task id from non task Executable Response");
      }
    }
  }
}
