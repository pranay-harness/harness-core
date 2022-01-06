/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.data.structure;

import java.util.Collection;
import java.util.Map;

/**
 * EmptyPredicate provides generic methods that are applicable for wide variety of constructs allowing for
 * static import of the method without risk of name collisions.
 */

public class SizeFunction {
  interface Size {
    int size();
  }

  public static <T extends Size> int size(T structure) {
    return structure == null ? 0 : structure.size();
  }

  public static <T> int size(Collection<T> collection) {
    return collection == null ? 0 : collection.size();
  }

  public static <K, V> int size(Map<K, V> map) {
    return map == null ? 0 : map.size();
  }

  public static int size(String string) {
    return string == null ? 0 : string.length();
  }

  public static int size(Object[] array) {
    return array == null ? 0 : array.length;
  }

  public static int size(long[] array) {
    return array == null ? 0 : array.length;
  }

  public static int size(int[] array) {
    return array == null ? 0 : array.length;
  }

  public static int size(short[] array) {
    return array == null ? 0 : array.length;
  }

  public static int size(char[] array) {
    return array == null ? 0 : array.length;
  }

  public static int size(byte[] array) {
    return array == null ? 0 : array.length;
  }

  public static int size(double[] array) {
    return array == null ? 0 : array.length;
  }

  public static int size(float[] array) {
    return array == null ? 0 : array.length;
  }

  public static int size(boolean[] array) {
    return array == null ? 0 : array.length;
  }
}
