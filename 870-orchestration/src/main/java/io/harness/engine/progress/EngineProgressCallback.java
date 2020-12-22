package io.harness.engine.progress;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.BinaryResponseData;
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
  @Inject KryoSerializer kryoSerializer;

  String nodeExecutionId;

  @Override
  public void notify(String correlationId, ProgressData progressData) {
    // TODO (prashant) : Do some thing better here right now to maintain backward compatibility.
    ProgressData data = null;
    if (progressData instanceof BinaryResponseData) {
      data = (ProgressData) kryoSerializer.asInflatedObject(((BinaryResponseData) progressData).getData());
    } else {
      data = progressData;
    }
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);

    Map<String, List<ProgressData>> progressDataMap = nodeExecution.getProgressDataMap();
    List<ProgressData> progressDataList = progressDataMap.getOrDefault(correlationId, new LinkedList<>());
    progressDataList.add(data);

    progressDataMap.putIfAbsent(correlationId, progressDataList);

    nodeExecutionService.update(nodeExecutionId, ops -> ops.set(NodeExecutionKeys.progressDataMap, progressDataMap));
  }
}
