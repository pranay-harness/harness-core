package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public enum TerraformCommandUnit {
  Plan,
  Apply,
  Adjust,
  Destroy,
  Rollback;
}
