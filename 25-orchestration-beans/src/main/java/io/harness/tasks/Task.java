package io.harness.tasks;

import javax.annotation.Nonnull;

public interface Task {
  String getUuid();

  String getWaitId();

  @Nonnull String getTaskIdentifier();

  @Nonnull String getTaskType();
}
