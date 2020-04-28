package software.wings.service.impl;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;
import software.wings.beans.trigger.Trigger;

public class TriggerLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(Trigger.class);

  public TriggerLogContext(String triggerId, OverrideBehavior behavior) {
    super(ID, triggerId, behavior);
  }
}
