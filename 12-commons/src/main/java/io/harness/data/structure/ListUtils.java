package io.harness.data.structure;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {
  public static List<String> trimList(List<Object> objects) {
    if (objects == null) {
      return null;
    }
    return objects.stream()
        .filter(o -> o != null)
        .map(o -> String.valueOf(o).trim().toLowerCase())
        .filter(s -> isNotEmpty(s))
        .distinct()
        .collect(toList());
  }

  public static List<String> trimStrings(List<String> strings) {
    if (strings == null) {
      return null;
    }
    return strings.stream()
        .filter(s -> s != null)
        .map(s -> s.trim().toLowerCase())
        .filter(s -> isNotEmpty(s))
        .distinct()
        .collect(toList());
  }

  public static <V> List<V> addSafely(V value, List<V> list) {
    // if value is null - do nothing
    if (value == null) {
      return list;
    }
    // If the list is null instantiate it
    if (list == null) {
      list = new ArrayList<>();
    }

    try {
      list.add(value);
    } catch (UnsupportedOperationException ignore) {
      // If the list does not allow for adding elements, instantiate copy that allows it.
      final ArrayList<V> arrayList = new ArrayList<>(list);
      arrayList.add(value);
      return arrayList;
    }
    return list;
  }
}
