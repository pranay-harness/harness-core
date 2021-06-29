package io.harness.pms.sdk.core.interrupt.publisher;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;

@OwnedBy(PIPELINE)
public interface SdkInterruptResponsePublisher {
  void publishEvent(
      String nodeExecutionId, InterruptType interruptType, String interruptId, InterruptConfig interruptConfig);
}
