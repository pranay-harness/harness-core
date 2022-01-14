package io.harness.limits.counter.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.Action;
import io.harness.limits.Counter;

import javax.annotation.Nullable;

@OwnedBy(PL)
public interface CounterService {
  @Nullable Counter get(Action action);

  Counter increment(Action action, int defaultValue);

  Counter upsert(Counter counter);
}
