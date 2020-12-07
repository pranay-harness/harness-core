package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.ExecutionMode;
import io.harness.pms.execution.NodeExecutionProto;

import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class NodeExecutionUtils {
  public boolean isChildSpawningMode(NodeExecutionProto proto) {
    ExecutionMode mode = proto.getMode();
    return mode == ExecutionMode.CHILD || mode == ExecutionMode.CHILDREN || mode == ExecutionMode.CHILD_CHAIN;
  }

  public boolean isTaskSpawningMode(NodeExecutionProto proto) {
    ExecutionMode mode = proto.getMode();
    return mode == ExecutionMode.TASK || mode == ExecutionMode.TASK_CHAIN;
  }

  public ExecutableResponse obtainLatestExecutableResponse(NodeExecutionProto proto) {
    List<ExecutableResponse> executableResponses = proto.getExecutableResponsesList();
    if (isEmpty(executableResponses)) {
      return null;
    }
    return executableResponses.get(executableResponses.size() - 1);
  }
}
