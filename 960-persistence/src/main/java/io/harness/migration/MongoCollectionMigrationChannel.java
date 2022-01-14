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
