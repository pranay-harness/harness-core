package io.harness.engine.progress.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.BinaryResponseData;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ProgressEventPublisher {
  String publishEvent(String nodeExecutionId, BinaryResponseData progressData);
}
