package io.harness.data.structure;

import java.util.Collection;
import java.util.Map;

/**
 * HasPredicate provides generic methods that are applicable for wide variety of constructs allowing for
 * static import of the method without risk of name collisions.
 */

public class HasPredicate {
  public interface HasNone {
    boolean hasNone();
  }

  public static <T extends HasNone> boolean hasNone(T structure) {
    return structure == null || structure.hasNone();
  }

  public static <T> boolean hasNone(Collection<T> collection) {
    return collection == null || collection.isEmpty();
  }

  public static <K, V> boolean hasNone(Map<K, V> map) {
    return map == null || map.isEmpty();
  }

  public static boolean hasNone(String string) {
    return string == null || string.isEmpty();
  }

  public static boolean hasNone(Object[] array) {
    return array == null || array.length == 0;
  }

  public static boolean hasNone(long[] array) {
    return array == null || array.length == 0;
  }

  public static boolean hasNone(int[] array) {
    return array == null || array.length == 0;
  }

  public static boolean hasNone(short[] array) {
    return array == null || array.length == 0;
  }

  public static boolean hasNone(char[] array) {
    return array == null || array.length == 0;
  }

  public static boolean hasNone(byte[] array) {
    return array == null || array.length == 0;
  }

  public static boolean hasNone(double[] array) {
    return array == null || array.length == 0;
  }

  public static boolean hasNone(float[] array) {
    return array == null || array.length == 0;
  }

  public static boolean hasNone(boolean[] array) {
    return array == null || array.length == 0;
  }

  public static <T extends HasNone> boolean hasSome(T structure) {
    return structure != null && !structure.hasNone();
  }

  public static <T> boolean hasSome(Collection<T> collection) {
    return collection != null && !collection.isEmpty();
  }

  public static <K, V> boolean hasSome(Map<K, V> map) {
    return map != null && !map.isEmpty();
  }

  public static boolean hasSome(String string) {
    return string != null && !string.isEmpty();
  }

  public static boolean hasSome(Object[] array) {
    return array != null && array.length != 0;
  }

  public static boolean hasSome(long[] array) {
    return array != null && array.length != 0;
  }

  public static boolean hasSome(int[] array) {
    return array != null && array.length != 0;
  }

  public static boolean hasSome(short[] array) {
    return array != null && array.length != 0;
  }

  public static boolean hasSome(char[] array) {
    return array != null && array.length != 0;
  }

  public static boolean hasSome(byte[] array) {
    return array != null && array.length != 0;
  }

  public static boolean hasSome(double[] array) {
    return array != null && array.length != 0;
  }

  public static boolean hasSome(float[] array) {
    return array != null && array.length != 0;
  }

  public static boolean hasSome(boolean[] array) {
    return array != null && array.length != 0;
  }
}
