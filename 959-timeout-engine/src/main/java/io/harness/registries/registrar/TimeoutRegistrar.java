/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
