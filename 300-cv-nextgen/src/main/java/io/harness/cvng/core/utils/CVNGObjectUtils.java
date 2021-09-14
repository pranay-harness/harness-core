/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.utils;

import java.util.Comparator;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
@UtilityClass
public class CVNGObjectUtils {
  /**
   * Handle nulls in a graceful manner and returns null if both the elements are null.
   */
  @Nullable
  public static <T> T max(T t1, T t2, Comparator<T> comparator) {
    if (t1 == null) {
      return t2;
    } else if (t2 == null) {
      return t1;
    } else {
      int v = comparator.compare(t1, t2);
      if (v > 0) {
        return t1;
      } else {
        return t2;
      }
    }
  }
}
