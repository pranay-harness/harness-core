package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.registrar.TimeoutRegistrar;
import io.harness.timeout.Dimension;
import io.harness.timeout.TimeoutTrackerFactory;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;
import io.harness.timeout.trackers.active.ActiveTimeoutTrackerFactory;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class TimeoutEngineTimeoutRegistrar implements TimeoutRegistrar {
  @Override
  public void register(Set<Pair<Dimension, Class<? extends TimeoutTrackerFactory>>> resolverClasses) {
    resolverClasses.add(Pair.of(AbsoluteTimeoutTrackerFactory.DIMENSION, AbsoluteTimeoutTrackerFactory.class));
    resolverClasses.add(Pair.of(ActiveTimeoutTrackerFactory.DIMENSION, ActiveTimeoutTrackerFactory.class));
  }
}
