package software.wings.service.impl;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;
import software.wings.beans.Workflow;

public class TriggerLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(Workflow.class);

  public TriggerLogContext(String triggerId, OverrideBehavior behavior) {
    super(ID, triggerId, behavior);
  }
}
