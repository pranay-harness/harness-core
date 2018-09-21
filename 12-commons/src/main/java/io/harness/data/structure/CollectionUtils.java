package io.harness.data.structure;

import java.util.Collection;

/**
 * Created by rsingh on 6/1/18.
 */
public class CollectionUtils {
  public static boolean isEqualCollection(Collection a, Collection b) {
    if (a == null && b != null) {
      return false;
    }

    if (a != null && b == null) {
      return false;
    }

    if (a == null || b == null) {
      return true;
    }

    if (a.size() != b.size()) {
      return false;
    }

    return a.containsAll(b) && b.containsAll(a);
  }
}
