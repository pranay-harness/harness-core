package io.harness.perpetualtask;

import lombok.Value;

import java.util.concurrent.Future;

@Value
public class PerpetualTaskHandle {
  private Future<?> taskHandle;
  private PerpetualTaskLifecycleManager taskLifecycleManager;
}
