package io.harness.feature.interfaces;

import java.time.temporal.TemporalUnit;

public interface SlidingWindow<T> {
  T getCurrentRate(String accountIdentifier, int duration, TemporalUnit unit);
}
