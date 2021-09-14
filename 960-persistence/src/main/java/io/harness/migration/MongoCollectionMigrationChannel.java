/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migration;

import io.harness.persistence.Store;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MongoCollectionMigrationChannel implements MigrationChannel {
  private Store store;
  private String collection;

  @Override
  public String getId() {
    return "mongodb://store/" + store.getName() + "/collection/" + collection;
  }
}
