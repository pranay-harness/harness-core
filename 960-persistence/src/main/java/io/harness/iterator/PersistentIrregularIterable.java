/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.iterator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Collections;
import java.util.List;

@OwnedBy(PL)
public interface PersistentIrregularIterable extends PersistentIterable {
  // Provides a list of iterations to handle. Note it returns a completely new list of iterations to replace the current
  // one. If some the current one are still valid they should be repeated.
  // Note that there is no limit on how many iterations can be provided. Since the list is updated as a second operation
  // such call can be limited with providing rich list that will caver multiple iterations.
  // Returning will keep the list as is without an update operation.
  List<Long> recalculateNextIterations(String fieldName, boolean skipMissed, long throttled);

  default boolean removeMissed(long now, List<Long> times) {
    int end = Collections.binarySearch(times, now);
    if (end < 0) {
      end = -end - 1;
    } else {
      while (end < times.size() && times.get(end) == now) {
        end++;
      }
    }
    times.subList(0, end).clear();
    return end > 0;
  }
}
