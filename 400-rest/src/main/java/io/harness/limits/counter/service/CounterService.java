package io.harness.limits.counter.service;

import io.harness.limits.Action;
import io.harness.limits.Counter;

import javax.annotation.Nullable;

public interface CounterService {
  @Nullable Counter get(Action action);

  Counter increment(Action action, int defaultValue);

  Counter upsert(Counter counter);
}
