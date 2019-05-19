package io.harness.limits.counter.service;

import com.google.inject.Inject;

import io.harness.limits.Action;
import io.harness.limits.Counter;
import io.harness.limits.Counter.CounterKeys;
import org.mongodb.morphia.FindAndModifyOptions;
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
    Query<Counter> query = wingsPersistence.createQuery(Counter.class).field(CounterKeys.KEY).equal(action.key());

    UpdateOperations<Counter> updateOperations = wingsPersistence.createUpdateOperations(Counter.class)
                                                     .inc(CounterKeys.VALUE, 1)
                                                     .setOnInsert(CounterKeys.VALUE, defaultValue);

    FindAndModifyOptions options = new FindAndModifyOptions();
    options.upsert(true);
    options.returnNew(true);

    return wingsPersistence.findAndModify(query, updateOperations, options);
  }

  @Override
  public Counter upsert(Counter counter) {
    wingsPersistence.save(counter);
    return counter;
  }
}
