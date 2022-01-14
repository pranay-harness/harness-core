package io.harness.resourcegroup.remote.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public enum ManagedFilter {
  NO_FILTER,
  ONLY_MANAGED,
  ONLY_CUSTOM;
}
