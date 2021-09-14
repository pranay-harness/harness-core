/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo;

import io.harness.logging.AutoLogContext;

public class CollectionLogContext extends AutoLogContext {
  public static final String ID = "collectionName";

  public CollectionLogContext(String collectionName, OverrideBehavior behavior) {
    super(ID, collectionName, behavior);
  }
}
