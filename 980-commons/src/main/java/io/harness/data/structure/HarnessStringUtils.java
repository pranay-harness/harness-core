package io.harness.data.structure;

import static io.harness.data.structure.HasPredicate.hasNone;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/*
Aim is to make sure that java 8 String.join is used over Guava Joiner class
 */
@UtilityClass
public class HarnessStringUtils {
  public static String join(@NonNull CharSequence delimiter, @NonNull CharSequence... elements) {
    return String.join(delimiter, elements);
  }
  public static String join(@NonNull CharSequence delimiter, @NonNull Iterable<? extends CharSequence> elements) {
    return String.join(delimiter, elements);
  }
  public static String nullIfEmpty(String stringInput) {
    return hasNone(stringInput) ? null : stringInput;
  }
}
