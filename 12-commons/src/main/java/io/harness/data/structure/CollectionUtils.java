package io.harness.data.structure;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

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

  /**
   * It goes over the
   * @param keyExtractor
   * @param <T>
   * @return
   */
  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
