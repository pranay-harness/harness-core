/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.iterator;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.System.currentTimeMillis;

import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;
import java.util.List;

@OwnedBy(PL)
public interface PersistentFibonacciIterable extends PersistentIrregularIterable {
  int INVENTORY_MINIMUM = 2;
  int REGULAR_INVENTORY = 10;

  default boolean recalculateTimestamps(
      List<Long> timestamps, boolean skipMissing, Long throttled, Duration coef, Duration max) {
    if (isEmpty(timestamps)) {
      long currentItem = throttled;

      long i_1 = 0;
      long i_2 = 1;
      while (i_2 * coef.toMillis() < max.toMillis()) {
        currentItem = currentItem + i_2 * coef.toMillis();
        timestamps.add(currentItem);
        long i_3 = i_1 + i_2;
        i_1 = i_2;
        i_2 = i_3;
      }
      return true;
    }

    long now = currentTimeMillis();

    // Take this item here, before we cleanup the list and potentially make it empty. We would like to align the items
    // based on the last previous one, instead of now, if they depend on each other.
    long time = isNotEmpty(timestamps) ? timestamps.get(timestamps.size() - 1) : now;

    boolean removed = skipMissing && removeMissed(now, timestamps);

    if (timestamps.size() >= INVENTORY_MINIMUM) {
      return removed;
    }

    long currentItem = time;
    while (timestamps.size() < REGULAR_INVENTORY) {
      currentItem = currentItem + max.toMillis();
      if (currentItem > now) {
        timestamps.add(currentItem);
      }
    }
    return true;
  }
}
