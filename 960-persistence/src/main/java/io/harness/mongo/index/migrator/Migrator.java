/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.index.migrator;

import org.mongodb.morphia.AdvancedDatastore;

public interface Migrator {
  void execute(AdvancedDatastore datastore);
}
