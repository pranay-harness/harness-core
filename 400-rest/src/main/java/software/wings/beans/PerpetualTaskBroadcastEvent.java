package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._420_DELEGATE_SERVICE)
public class PerpetualTaskBroadcastEvent {
  private String eventType;
  private String broadcastDelegateId;
}
