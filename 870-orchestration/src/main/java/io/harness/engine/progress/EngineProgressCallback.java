package io.harness.engine.progress;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.tasks.ProgressData;
import io.harness.waiter.ProgressCallback;

import com.google.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EngineProgressCallback implements ProgressCallback {
  @Inject NodeExecutionService nodeExecutionService;

  String nodeExecutionId;

  @Override
  public void notify(String correlationId, ProgressData progressData) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);

    Map<String, List<ProgressData>> progressDataMap = nodeExecution.getProgressDataMap();
    List<ProgressData> progressDataList = progressDataMap.getOrDefault(correlationId, new LinkedList<>());
    progressDataList.add(progressData);

    progressDataMap.putIfAbsent(correlationId, progressDataList);

    nodeExecutionService.update(nodeExecutionId, ops -> ops.set(NodeExecutionKeys.progressDataMap, progressDataMap));
  }
}
