package io.harness.cvng.activity.services.api;

import io.harness.cvng.activity.entities.Activity;

public abstract class ActivityUpdateHandler<T extends Activity> {
  public abstract void handleCreate(T activity);
  public abstract void handleDelete(T activity);
  public abstract void handleUpdate(T existingActivity, T newActivity);
}
