/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.Registrar;
import io.harness.timeout.TimeoutTrackerFactory;
import io.harness.timeout.contracts.Dimension;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface TimeoutRegistrar extends Registrar<Dimension, TimeoutTrackerFactory<?>> {
  void register(Set<Pair<Dimension, TimeoutTrackerFactory<?>>> adviserClasses);
}
