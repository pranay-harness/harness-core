package io.harness.perpetualtask;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.concurrent.Future;
import lombok.Value;

@Value
@TargetModule(Module._420_DELEGATE_AGENT)
public class PerpetualTaskHandle {
  private Future<?> taskHandle;
  private PerpetualTaskLifecycleManager taskLifecycleManager;
}
