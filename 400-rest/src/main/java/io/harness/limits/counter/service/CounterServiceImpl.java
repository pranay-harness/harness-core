package io.harness.limits.counter.service;

import com.google.inject.Inject;
import io.harness.limits.Action;
import io.harness.limits.Counter;
import io.harness.limits.Counter.CounterKeys;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;

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
