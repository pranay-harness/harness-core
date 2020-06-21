package io.harness.data.structure;

import static io.harness.data.structure.ListUtils.OneAndOnlyOne.MANY;
import static io.harness.data.structure.ListUtils.OneAndOnlyOne.NONE;
import static io.harness.data.structure.ListUtils.OneAndOnlyOne.ONE;
import static java.util.stream.Collectors.toList;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class ListUtils {
  public static List<String> trimStrings(List<String> strings) {
    if (strings == null) {
      return null;
    }
    return strings.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(EmptyPredicate::isNotEmpty)
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

  public enum OneAndOnlyOne { NONE, ONE, MANY }

  public static OneAndOnlyOne oneAndOnlyOne(Object... values) {
    int count = 0;
    for (Object value : values) {
      if (value != null) {
        if (++count > 1) {
          return MANY;
        }
      }
    }
    if (count == 0) {
      return NONE;
    }
    return ONE;
  }
}
