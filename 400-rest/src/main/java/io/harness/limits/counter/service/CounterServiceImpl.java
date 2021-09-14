/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits.counter.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.Action;
import io.harness.limits.Counter;
import io.harness.limits.Counter.CounterKeys;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(PL)
public class CounterServiceImpl implements CounterService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public Counter get(Action action) {
    return wingsPersistence.createQuery(Counter.class).field("key").equal(action.key()).get();
  }

  @Override
  public Counter increment(Action action, int defaultValue) {
    Query<Counter> query = wingsPersistence.createQuery(Counter.class).field(CounterKeys.key).equal(action.key());

    UpdateOperations<Counter> updateOperations = wingsPersistence.createUpdateOperations(Counter.class)
                                                     .inc(CounterKeys.value, 1)
                                                     .setOnInsert(CounterKeys.value, defaultValue);

    return wingsPersistence.findAndModify(query, updateOperations, WingsPersistence.upsertReturnNewOptions);
  }

  @Override
  public Counter upsert(Counter counter) {
    Query<Counter> query = wingsPersistence.createQuery(Counter.class).field(CounterKeys.key).equal(counter.getKey());
    UpdateOperations<Counter> update =
        wingsPersistence.createUpdateOperations(Counter.class).set(CounterKeys.value, counter.getValue());
    wingsPersistence.findAndModify(query, update, WingsPersistence.upsertReturnNewOptions);
    return counter;
  }
}
