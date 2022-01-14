package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

import software.wings.beans.trigger.Trigger;

@OwnedBy(CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class TriggerLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(Trigger.class);

  public TriggerLogContext(String triggerId, OverrideBehavior behavior) {
    super(ID, triggerId, behavior);
  }
}
