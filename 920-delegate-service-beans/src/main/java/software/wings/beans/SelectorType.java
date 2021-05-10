package software.wings.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DEL)
public enum SelectorType {
  PROFILE_NAME,
  DELEGATE_NAME,
  HOST_NAME,
  GROUP_NAME,
  PROFILE_SELECTORS
}
