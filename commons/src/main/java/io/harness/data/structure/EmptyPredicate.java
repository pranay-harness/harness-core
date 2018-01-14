package io.harness.data.structure;

import java.util.Collection;
import java.util.Map;

/**
 * EmptyPredicate provides generic methods that are applicable for wide variety of constructs allowing for
 * static import of the method without risk of name collisions.
 */

public class EmptyPredicate {
  interface IsEmpty {
    boolean isEmpty();
  }

  public static <T extends IsEmpty> boolean isEmpty(T structure) {
    return structure == null || structure.isEmpty();
  }

  public static boolean isEmpty(Collection collection) {
    return collection == null || collection.isEmpty();
  }

  public static boolean isEmpty(Map map) {
    return map == null || map.isEmpty();
  }

  public static boolean isEmpty(String string) {
    return string == null || string.isEmpty();
  }

  public static <T extends IsEmpty> boolean isNotEmpty(T structure) {
    return structure != null && !structure.isEmpty();
  }

  public static boolean isNotEmpty(Collection collection) {
    return collection != null && !collection.isEmpty();
  }

  public static boolean isNotEmpty(Map map) {
    return map != null && !map.isEmpty();
  }

  public static boolean isNotEmpty(String string) {
    return string != null && !string.isEmpty();
  }
}
