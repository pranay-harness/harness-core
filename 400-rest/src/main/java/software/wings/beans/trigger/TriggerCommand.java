package software.wings.beans.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public interface TriggerCommand {
  TriggerCommandType getTriggerCommandType();

  enum TriggerCommandType {
    DEPLOYMENT_NEEDED_CHECK,
  }
}
