package io.harness.limits.counter.service;

import com.google.inject.Inject;

import io.harness.limits.Action;
import io.harness.limits.Counter;
import software.wings.dl.WingsPersistence;

public class CounterServiceImpl implements CounterService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public Counter get(Action action) {
    return wingsPersistence.createQuery(Counter.class).field("key").equal(action.key()).get();
  }
}
