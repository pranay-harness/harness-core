package io.harness.data.structure;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.util.List;
import java.util.stream.Collectors;

public class ListUtil {
  public static List<String> trimList(List<Object> objects) {
    if (objects == null) {
      return null;
    }
    return objects.stream()
        .filter(o -> o != null)
        .map(o -> String.valueOf(o).trim().toLowerCase())
        .filter(s -> isNotEmpty(s))
        .distinct()
        .collect(Collectors.toList());
  }
}
