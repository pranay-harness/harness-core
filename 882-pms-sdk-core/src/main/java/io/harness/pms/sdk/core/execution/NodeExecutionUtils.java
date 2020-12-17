package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.expression.OrchestrationField;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;

import java.util.List;
import java.util.Map;
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

  public static int retryCount(NodeExecutionProto nodeExecutionProto) {
    if (isRetry(nodeExecutionProto)) {
      return nodeExecutionProto.getRetryIdsList().size();
    }
    return 0;
  }

  private static boolean isRetry(NodeExecutionProto nodeExecutionProto) {
    return !isEmpty(nodeExecutionProto.getRetryIdsList());
  }

  public ExecutableResponse obtainLatestExecutableResponse(NodeExecutionProto proto) {
    List<ExecutableResponse> executableResponses = proto.getExecutableResponsesList();
    if (isEmpty(executableResponses)) {
      return null;
    }
    return executableResponses.get(executableResponses.size() - 1);
  }

  public Map<String, Object> extractStepParameters(String json) {
    if (EmptyPredicate.isEmpty(json)) {
      return null;
    }
    Map<String, Object> map = JsonOrchestrationUtils.asMap(json);
    updateStepParametersMap(map);
    return map;
  }

  private void updateStepParametersMap(Map<String, Object> map) {
    if (EmptyPredicate.isEmpty(map)) {
      return;
    }

    map.forEach((k, v) -> {
      if (!(v instanceof Map)) {
        return;
      }

      Map<String, Object> inner = (Map<String, Object>) v;
      if (inner.containsKey(OrchestrationField.ORCHESTRATION_FIELD_CLASS_FIELD)) {
        String className = (String) inner.get(OrchestrationField.ORCHESTRATION_FIELD_CLASS_FIELD);
        Class<? extends OrchestrationField> aClass;
        try {
          aClass = (Class<? extends OrchestrationField>) Class.forName(className);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
        OrchestrationField field = JsonOrchestrationUtils.asObject(JsonOrchestrationUtils.asJson(v), aClass);
        map.put(k, field);
        Object finalValue = field.fetchFinalValue();
        if (finalValue instanceof Map) {
          updateStepParametersMap((Map<String, Object>) finalValue);
        }
      } else {
        updateStepParametersMap(inner);
      }
    });
  }
}
