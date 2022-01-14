package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.entities.changeSource.ChangeSource;

public abstract class ChangeSourceUpdateHandler<T extends ChangeSource> {
  public abstract void handleCreate(T changeSource);
  public abstract void handleDelete(T changeSource);
  public abstract void handleUpdate(T existingChangeSource, T newChangeSource);
}
