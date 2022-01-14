package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

/**
 * Created by sgurubelli on 10/25/17.
 */
@OwnedBy(CDC)
public enum TriggerConditionType {
  NEW_ARTIFACT,
  PIPELINE_COMPLETION,
  SCHEDULED,
  WEBHOOK,
  NEW_INSTANCE,
  NEW_MANIFEST
}
