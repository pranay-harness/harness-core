package io.harness.mongo;

import io.harness.logging.AutoLogContext;

public class CollectionLogContext extends AutoLogContext {
  public static final String ID = "collectionName";

  public CollectionLogContext(String collectionName, OverrideBehavior behavior) {
    super(ID, collectionName, behavior);
  }
}
