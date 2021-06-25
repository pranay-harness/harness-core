package io.harness.pms.sdk.execution.events.interrupts.publisher;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptType;

@OwnedBy(PIPELINE)
public interface InterruptEventNotifyPublisher {
  String publishEvent(String uuid, InterruptType interruptType);
}
