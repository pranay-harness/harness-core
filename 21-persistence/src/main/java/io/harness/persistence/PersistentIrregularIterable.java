package io.harness.persistence;

import java.util.List;

public interface PersistentIrregularIterable extends PersistentIterable {
  // Provides a list of iterations to handle. Note it returns a completely new list of iterations to replace the current
  // one. If some the current one are still valid they should be repeated.
  // Note that there is no limit on how many iterations can be provided. Since the list is updated as a second operation
  // such call can be limited with providing rich list that will caver multiple iterations.
  // Returning will keep the list as is without an update operation.
  List<Long> recalculateNextIterations(String fieldName);
}
