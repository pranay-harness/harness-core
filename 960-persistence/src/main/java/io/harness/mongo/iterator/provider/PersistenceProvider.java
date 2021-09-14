/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.iterator.provider;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType;

import io.harness.iterator.PersistentIterable;
import io.harness.mongo.iterator.filter.FilterExpander;

import java.time.Duration;
import java.util.List;

public interface PersistenceProvider<T extends PersistentIterable, F extends FilterExpander> {
  void updateEntityField(T entity, List<Long> nextIterations, Class<T> clazz, String fieldName);
  T obtainNextInstance(long base, long throttled, Class<T> clazz, String fieldName, SchedulingType schedulingType,
      Duration targetInterval, F filterExpander);
  T findInstance(Class<T> clazz, String fieldName, F filterExpander);
  void recoverAfterPause(Class<T> clazz, String fieldName);
}
