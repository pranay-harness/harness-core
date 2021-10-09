package io.harness.accesscontrol.support;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class SupportPreference {
  String accountIdentifier;
  boolean isSupportEnabled;
}
