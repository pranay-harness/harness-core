package software.wings.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.DEL)
public class PerpetualTaskBroadcastEvent {
  private String eventType;
  private String broadcastDelegateId;
}
