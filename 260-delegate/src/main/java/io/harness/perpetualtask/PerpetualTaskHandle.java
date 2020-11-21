package io.harness.perpetualtask;

import java.util.concurrent.Future;
import lombok.Value;

@Value
public class PerpetualTaskHandle {
  private Future<?> taskHandle;
  private PerpetualTaskLifecycleManager taskLifecycleManager;
}
