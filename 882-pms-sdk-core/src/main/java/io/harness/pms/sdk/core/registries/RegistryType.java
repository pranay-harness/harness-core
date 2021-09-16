package io.harness.pms.sdk.core.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum RegistryType {
  STEP,
  ADVISER,
  RESOLVER,
  FACILITATOR,
  ORCHESTRATION_EVENT,
  ORCHESTRATION_FIELD,
  SDK_FUNCTOR
}
