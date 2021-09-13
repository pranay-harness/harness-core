package io.harness.ng.core.user;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.GTM)
public enum SignupAction {
  REGULAR,
  TRIAL,
  SUBSCRIBE,
}
