package io.harness.ngtriggers.events;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CI)
public class TriggerOutboxEvents {
  public static final String TRIGGER_CREATED = "TriggerCreated";
  public static final String TRIGGER_UPDATED = "TriggerUpdated";
  public static final String TRIGGER_DELETED = "TriggerDeleted";
}
