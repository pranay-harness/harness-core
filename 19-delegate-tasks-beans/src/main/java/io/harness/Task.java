package io.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import javax.annotation.Nonnull;

@OwnedBy(CDC)
public interface Task {
  String getWaitId();

  @Nonnull String getTaskIdentifier();
}
