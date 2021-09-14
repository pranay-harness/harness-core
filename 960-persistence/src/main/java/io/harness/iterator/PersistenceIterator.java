/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.iterator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public interface PersistenceIterator<T extends PersistentIterable> {
  enum ProcessMode { LOOP, PUMP }

  void wakeup();
  void process();
  void recoverAfterPause();
}
