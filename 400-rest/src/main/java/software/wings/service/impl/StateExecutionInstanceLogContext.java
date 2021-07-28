package software.wings.service.impl;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

import software.wings.sm.StateExecutionInstance;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class StateExecutionInstanceLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(StateExecutionInstance.class);

  public StateExecutionInstanceLogContext(String stateExecutionInstanceId, OverrideBehavior behavior) {
    super(ID, stateExecutionInstanceId, behavior);
  }
}
