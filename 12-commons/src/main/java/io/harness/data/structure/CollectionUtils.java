package io.harness.data.structure;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

  public static <T> List<T> emptyIfNull(List<T> collection) {
    return collection == null ? new ArrayList<>() : collection;
  }

  public static Set<String> trimmedLowercaseSet(Collection<String> strings) {
    if (strings == null) {
      return null;
    }
    return strings.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(EmptyPredicate::isNotEmpty)
        .map(String::toLowerCase)
        .collect(toSet());
  }
}
