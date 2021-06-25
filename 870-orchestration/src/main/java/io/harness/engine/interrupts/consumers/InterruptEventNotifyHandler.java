package io.harness.engine.interrupts.consumers;

import static io.harness.govern.Switch.noop;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.interrupts.InterruptEventNotifyProto;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.tasks.BinaryResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class InterruptEventNotifyHandler extends PmsBaseEventHandler<InterruptEventNotifyProto> {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  protected @NonNull Map<String, String> extraLogProperties(InterruptEventNotifyProto event) {
    return ImmutableMap.<String, String>builder()
        .put("interruptType", event.getInterruptType().name())
        .put("notifyId", event.getNotifyId())
        .build();
  }

  @Override
  protected Ambiance extractAmbiance(InterruptEventNotifyProto event) {
    return Ambiance.newBuilder().build();
  }

  @Override
  protected Map<String, String> extractMetricContext(InterruptEventNotifyProto event) {
    return ImmutableMap.of();
  }

  @Override
  protected String getMetricPrefix(InterruptEventNotifyProto message) {
    return null;
  }

  @Override
  protected void handleEventWithContext(InterruptEventNotifyProto event) {
    switch (event.getInterruptType()) {
      case ABORT:
      case CUSTOM_FAILURE:
        waitNotifyEngine.doneWith(event.getNotifyId(), BinaryResponseData.builder().build());
        log.info("[PIPELINE] Handled InterruptEvent Notify Successfully for type - " + event.getInterruptType().name());
        return;
      default:
        log.warn("No Handling present for Interrupt Event Notify of type : {}", event.getInterruptType());
        noop();
    }
  }
}
