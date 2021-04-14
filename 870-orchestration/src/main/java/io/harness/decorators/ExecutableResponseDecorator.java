package io.harness.decorators;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.TaskExecutableResponse;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class ExecutableResponseDecorator {
  String taskId;

  public ExecutableResponse decorate(ExecutableResponse executableResponse) {
    if (executableResponse.hasTask()) {
      TaskExecutableResponse taskExecutableResponse =
          executableResponse.getTask().toBuilder().setTaskId(taskId).build();
      return executableResponse.toBuilder().setTask(taskExecutableResponse).build();
    } else if (executableResponse.hasTaskChain()) {
      TaskChainExecutableResponse taskChainExecutableResponse =
          executableResponse.getTaskChain().toBuilder().setTaskId(taskId).build();
      return executableResponse.toBuilder().setTaskChain(taskChainExecutableResponse).build();
    }
    return executableResponse;
  }
}
