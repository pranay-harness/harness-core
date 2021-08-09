package io.harness.mongo.index.migrator;

import org.mongodb.morphia.AdvancedDatastore;

public interface Migrator {
  void execute(AdvancedDatastore datastore);
}
