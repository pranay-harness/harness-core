package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.trigger.WebhookSource.WebhookEvent;

import java.util.List;

@OwnedBy(CDC)
public interface GitEventType {
  List<WebhookEvent> getEventTypes();
}
