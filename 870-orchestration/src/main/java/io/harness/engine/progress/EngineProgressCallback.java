package io.harness.engine.progress;

import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ProgressData;
import io.harness.waiter.ProgressCallback;

import com.google.inject.Inject;
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
    if (!(progressData instanceof BinaryResponseData)) {
      throw new UnsupportedOperationException("Progress updates are not supported for raw non Binary Response Data");
    }
    ProgressData data = (ProgressData) kryoSerializer.asInflatedObject(((BinaryResponseData) progressData).getData());
    if (data instanceof UnitProgressData) {
      ProgressData finalData = data;
      nodeExecutionService.update(nodeExecutionId,
          ops -> ops.set(NodeExecutionKeys.unitProgresses, ((UnitProgressData) finalData).getUnitProgresses()));
    }
  }
}
