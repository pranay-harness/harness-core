package io.harness.pms.execution.utils;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.interrupts.InterruptEvent;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InterruptEventUtils {
  public AutoLogContext obtainLogContext(InterruptEvent event) {
    return new AutoLogContext(logContextMap(event), OVERRIDE_NESTS);
  }

  private Map<String, String> logContextMap(InterruptEvent event) {
    Map<String, String> logContext = new HashMap<>(AmbianceUtils.logContextMap(event.getNodeExecution().getAmbiance()));
    logContext.put("interruptType", event.getType().name());
    logContext.put("interruptUuid", event.getInterruptUuid());
    logContext.put("notifyId", event.getNotifyId());
    return logContext;
  }
}
