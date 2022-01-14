package software.wings.beans.command;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * Created by rishi on 1/29/17.
 */
@OwnedBy(HarnessTeam.CDC)
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
