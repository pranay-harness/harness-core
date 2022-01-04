package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.DEL)
public class UpgradeCheckResult {
  private final String imageTag;
  private final boolean shouldUpgrade;
}
