package io.harness.pms.sdk.core.interrupt.publisher;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptType;

@OwnedBy(PIPELINE)
public interface SdkInterruptEventNotifyPublisher {
  void publishEvent(String notifyId, InterruptType interruptType);
}
