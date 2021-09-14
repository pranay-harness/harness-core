/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.progress;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.progress.publisher.ProgressEventPublisher;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ProgressData;
import io.harness.waiter.ProgressCallback;

import com.google.inject.Inject;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Transient;

@Value
@Builder
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class EngineProgressCallback implements ProgressCallback {
  @Inject @Transient NodeExecutionService nodeExecutionService;
  @Inject @Transient KryoSerializer kryoSerializer;
  @Inject @Transient ProgressEventPublisher progressEventPublisher;

  String nodeExecutionId;

  @Override
  public void notify(String correlationId, ProgressData progressData) {
    if (!(progressData instanceof BinaryResponseData)) {
      throw new UnsupportedOperationException("Progress updates are not supported for raw non Binary Response Data");
    }

    // This is the new way of managing progress updates below code is only to maintain backward compatibility
    progressEventPublisher.publishEvent(nodeExecutionId, (BinaryResponseData) progressData);

    try {
      // This code is only to maintain backward compatibility
      ProgressData data = (ProgressData) kryoSerializer.asInflatedObject(((BinaryResponseData) progressData).getData());
      if (data instanceof UnitProgressData) {
        nodeExecutionService.update(nodeExecutionId,
            ops -> ops.set(NodeExecutionKeys.unitProgresses, ((UnitProgressData) data).getUnitProgresses()));
      }
      log.info("Node Execution updated for progress data");
    } catch (Exception ex) {
      log.error("Failed to deserialize progress data via kryo");
    }
  }
}
