/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.iterator.filter;

import org.mongodb.morphia.query.Query;

public interface MorphiaFilterExpander<T> extends FilterExpander {
  void filter(Query<T> query);
}
