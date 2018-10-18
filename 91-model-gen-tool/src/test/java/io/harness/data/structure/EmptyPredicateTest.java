package io.harness.data.structure;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.harness.CategoryTest;
import io.harness.category.FastUnitTests;
import io.harness.data.structure.EmptyPredicate.IsEmpty;
import lombok.Builder;
import lombok.Getter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmptyPredicateTest extends CategoryTest {
  @Builder
  @Getter
  static class Custom implements IsEmpty {
    boolean empty;
  }

  @Test
  @Category({FastUnitTests.class})
  public void testNull() {
    Custom custom = null;
    assertTrue(isEmpty(custom));
    assertFalse(isNotEmpty(custom));

    List<Integer> list = null;
    assertTrue(isEmpty(list));
    assertFalse(isNotEmpty(list));

    String string = null;
    assertTrue(isEmpty(string));
    assertFalse(isNotEmpty(string));

    Map map = null;
    assertTrue(isEmpty(map));
    assertFalse(isNotEmpty(map));

    ImplementsCollection<String> collection = null;
    assertTrue(isEmpty(collection));
    assertFalse(isNotEmpty(collection));

    ImplementsMap<String, String> implMap = null;
    assertTrue(isEmpty(implMap));
    assertFalse(isNotEmpty(implMap));

    Object[] objects = null;
    assertTrue(isEmpty(objects));
    assertFalse(isNotEmpty(objects));

    long[] longs = null;
    assertTrue(isEmpty(longs));
    assertFalse(isNotEmpty(longs));

    int[] ints = null;
    assertTrue(isEmpty(ints));
    assertFalse(isNotEmpty(ints));

    short[] shorts = null;
    assertTrue(isEmpty(shorts));
    assertFalse(isNotEmpty(shorts));

    char[] chars = null;
    assertTrue(isEmpty(chars));
    assertFalse(isNotEmpty(chars));

    byte[] bytes = null;
    assertTrue(isEmpty(bytes));
    assertFalse(isNotEmpty(bytes));

    double[] doubles = null;
    assertTrue(isEmpty(doubles));
    assertFalse(isNotEmpty(doubles));

    float[] floats = null;
    assertTrue(isEmpty(floats));
    assertFalse(isNotEmpty(floats));

    boolean[] booleans = null;
    assertTrue(isEmpty(booleans));
    assertFalse(isNotEmpty(booleans));
  }

  @Test
  @Category({FastUnitTests.class})
  public void testEmpty() {
    Custom custom = Custom.builder().empty(true).build();
    assertTrue(isEmpty(custom));
    assertFalse(isNotEmpty(custom));

    List<String> list = new ArrayList();
    assertTrue(isEmpty(list));
    assertFalse(isNotEmpty(list));

    String string = "";
    assertTrue(isEmpty(string));
    assertFalse(isNotEmpty(string));

    Map<String, String> map = new HashMap();
    assertTrue(isEmpty(map));
    assertFalse(isNotEmpty(map));

    ImplementsCollection<String> collection = new ImplementsCollection(true);
    assertTrue(isEmpty(collection));
    assertFalse(isNotEmpty(collection));

    ImplementsMap<String, String> implMap = new ImplementsMap<>(true);
    assertTrue(isEmpty(implMap));
    assertFalse(isNotEmpty(implMap));

    Object[] objects = new Object[0];
    assertTrue(isEmpty(objects));
    assertFalse(isNotEmpty(objects));

    long[] longs = new long[0];
    assertTrue(isEmpty(longs));
    assertFalse(isNotEmpty(longs));

    int[] ints = new int[0];
    assertTrue(isEmpty(ints));
    assertFalse(isNotEmpty(ints));

    short[] shorts = new short[0];
    assertTrue(isEmpty(shorts));
    assertFalse(isNotEmpty(shorts));

    char[] chars = new char[0];
    assertTrue(isEmpty(chars));
    assertFalse(isNotEmpty(chars));

    byte[] bytes = new byte[0];
    assertTrue(isEmpty(bytes));
    assertFalse(isNotEmpty(bytes));

    double[] doubles = new double[0];
    assertTrue(isEmpty(doubles));
    assertFalse(isNotEmpty(doubles));

    float[] floats = new float[0];
    assertTrue(isEmpty(floats));
    assertFalse(isNotEmpty(floats));

    boolean[] booleans = new boolean[0];
    assertTrue(isEmpty(booleans));
    assertFalse(isNotEmpty(booleans));
  }

  @Test
  @Category({FastUnitTests.class})
  public void testNotEmpty() {
    Custom custom = Custom.builder().empty(false).build();
    assertFalse(isEmpty(custom));
    assertTrue(isNotEmpty(custom));

    List<String> list = asList("foo");
    assertFalse(isEmpty(list));
    assertTrue(isNotEmpty(list));

    String string = "some";
    assertFalse(isEmpty(string));
    assertTrue(isNotEmpty(string));

    Map<String, String> map = new HashMap() {
      { put("foo", "foo"); }
    };
    assertFalse(isEmpty(map));
    assertTrue(isNotEmpty(map));

    ImplementsCollection<String> collection = new ImplementsCollection(false);
    assertFalse(isEmpty(collection));
    assertTrue(isNotEmpty(collection));

    ImplementsMap<String, String> implMap = new ImplementsMap<>(false);
    assertFalse(isEmpty(implMap));
    assertTrue(isNotEmpty(implMap));

    Object[] objects = new Object[1];
    assertFalse(isEmpty(objects));
    assertTrue(isNotEmpty(objects));

    long[] longs = new long[1];
    assertFalse(isEmpty(longs));
    assertTrue(isNotEmpty(longs));

    int[] ints = new int[1];
    assertFalse(isEmpty(ints));
    assertTrue(isNotEmpty(ints));

    short[] shorts = new short[1];
    assertFalse(isEmpty(shorts));
    assertTrue(isNotEmpty(shorts));

    char[] chars = new char[1];
    assertFalse(isEmpty(chars));
    assertTrue(isNotEmpty(chars));

    byte[] bytes = new byte[1];
    assertFalse(isEmpty(bytes));
    assertTrue(isNotEmpty(bytes));

    double[] doubles = new double[1];
    assertFalse(isEmpty(doubles));
    assertTrue(isNotEmpty(doubles));

    float[] floats = new float[1];
    assertFalse(isEmpty(floats));
    assertTrue(isNotEmpty(floats));

    boolean[] booleans = new boolean[1];
    assertFalse(isEmpty(booleans));
    assertTrue(isNotEmpty(booleans));
  }
}
