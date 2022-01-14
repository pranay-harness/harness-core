package software.wings.beans.loginSettings;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public enum PasswordSource {
  SIGN_UP_FLOW,
  PASSWORD_RESET_FLOW;
}
