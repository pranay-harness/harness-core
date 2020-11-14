package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.Registrar;
import io.harness.timeout.Dimension;
import io.harness.timeout.TimeoutTrackerFactory;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public interface TimeoutRegistrar extends Registrar<Dimension, TimeoutTrackerFactory<?>> {
  void register(Set<Pair<Dimension, TimeoutTrackerFactory<?>>> adviserClasses);
}
