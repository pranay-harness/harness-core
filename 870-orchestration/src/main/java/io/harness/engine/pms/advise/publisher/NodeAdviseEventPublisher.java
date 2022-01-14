package io.harness.engine.pms.advise.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.Status;

@OwnedBy(HarnessTeam.PIPELINE)
public interface NodeAdviseEventPublisher {
  String publishEvent(String nodeExecutionId, Status fromStatus);
}
