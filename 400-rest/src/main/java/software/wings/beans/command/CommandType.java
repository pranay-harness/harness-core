package software.wings.beans.command;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * Created by rishi on 1/29/17.
 */
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public enum CommandType {

  START,
  STOP,
  INSTALL,
  ENABLE,
  DISABLE,
  VERIFY,
  OTHER,
  RESIZE,
  SETUP
}
