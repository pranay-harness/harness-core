package io.harness.tasks;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import javax.annotation.Nonnull;

@OwnedBy(CDC)
public interface Task {
  String getUuid();

  String getWaitId();

  @Nonnull String getTaskIdentifier();

  @Nonnull String getTaskType();
}
