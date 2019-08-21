package io.harness.data.structure;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

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
  @Getter
  @Builder
  static class Custom implements IsEmpty {
    boolean empty;
  }

  @Test
  @Category({FastUnitTests.class})
  public void testNull() {
    Custom custom = null;
    assertThat(isEmpty(custom)).isTrue();
    assertFalse(isNotEmpty(custom));

    List<Integer> list = null;
    assertThat(isEmpty(list)).isTrue();
    assertFalse(isNotEmpty(list));

    String string = null;
    assertThat(isEmpty(string)).isTrue();
    assertFalse(isNotEmpty(string));

    Map map = null;
    assertThat(isEmpty(map)).isTrue();
    assertFalse(isNotEmpty(map));

    ImplementsCollection<String> collection = null;
    assertThat(isEmpty(collection)).isTrue();
    assertFalse(isNotEmpty(collection));

    ImplementsMap<String, String> implMap = null;
    assertThat(isEmpty(implMap)).isTrue();
    assertFalse(isNotEmpty(implMap));

    Object[] objects = null;
    assertThat(isEmpty(objects)).isTrue();
    assertFalse(isNotEmpty(objects));

    long[] longs = null;
    assertThat(isEmpty(longs)).isTrue();
    assertFalse(isNotEmpty(longs));

    int[] ints = null;
    assertThat(isEmpty(ints)).isTrue();
    assertFalse(isNotEmpty(ints));

    short[] shorts = null;
    assertThat(isEmpty(shorts)).isTrue();
    assertFalse(isNotEmpty(shorts));

    char[] chars = null;
    assertThat(isEmpty(chars)).isTrue();
    assertFalse(isNotEmpty(chars));

    byte[] bytes = null;
    assertThat(isEmpty(bytes)).isTrue();
    assertFalse(isNotEmpty(bytes));

    double[] doubles = null;
    assertThat(isEmpty(doubles)).isTrue();
    assertFalse(isNotEmpty(doubles));

    float[] floats = null;
    assertThat(isEmpty(floats)).isTrue();
    assertFalse(isNotEmpty(floats));

    boolean[] booleans = null;
    assertThat(isEmpty(booleans)).isTrue();
    assertFalse(isNotEmpty(booleans));
  }

  @Test
  @Category({FastUnitTests.class})
  public void testEmpty() {
    Custom custom = Custom.builder().empty(true).build();
    assertThat(isEmpty(custom)).isTrue();
    assertFalse(isNotEmpty(custom));

    List<String> list = new ArrayList();
    assertThat(isEmpty(list)).isTrue();
    assertFalse(isNotEmpty(list));

    String string = "";
    assertThat(isEmpty(string)).isTrue();
    assertFalse(isNotEmpty(string));

    Map<String, String> map = new HashMap();
    assertThat(isEmpty(map)).isTrue();
    assertFalse(isNotEmpty(map));

    ImplementsCollection<String> collection = new ImplementsCollection(true);
    assertThat(isEmpty(collection)).isTrue();
    assertFalse(isNotEmpty(collection));

    ImplementsMap<String, String> implMap = new ImplementsMap<>(true);
    assertThat(isEmpty(implMap)).isTrue();
    assertFalse(isNotEmpty(implMap));

    Object[] objects = new Object[0];
    assertThat(isEmpty(objects)).isTrue();
    assertFalse(isNotEmpty(objects));

    long[] longs = new long[0];
    assertThat(isEmpty(longs)).isTrue();
    assertFalse(isNotEmpty(longs));

    int[] ints = new int[0];
    assertThat(isEmpty(ints)).isTrue();
    assertFalse(isNotEmpty(ints));

    short[] shorts = new short[0];
    assertThat(isEmpty(shorts)).isTrue();
    assertFalse(isNotEmpty(shorts));

    char[] chars = new char[0];
    assertThat(isEmpty(chars)).isTrue();
    assertFalse(isNotEmpty(chars));

    byte[] bytes = new byte[0];
    assertThat(isEmpty(bytes)).isTrue();
    assertFalse(isNotEmpty(bytes));

    double[] doubles = new double[0];
    assertThat(isEmpty(doubles)).isTrue();
    assertFalse(isNotEmpty(doubles));

    float[] floats = new float[0];
    assertThat(isEmpty(floats)).isTrue();
    assertFalse(isNotEmpty(floats));

    boolean[] booleans = new boolean[0];
    assertThat(isEmpty(booleans)).isTrue();
    assertFalse(isNotEmpty(booleans));
  }

  @Test
  @Category({FastUnitTests.class})
  public void testNotEmpty() {
    Custom custom = Custom.builder().empty(false).build();
    assertFalse(isEmpty(custom));
    assertThat(isNotEmpty(custom)).isTrue();

    List<String> list = asList("foo");
    assertFalse(isEmpty(list));
    assertThat(isNotEmpty(list)).isTrue();

    String string = "some";
    assertFalse(isEmpty(string));
    assertThat(isNotEmpty(string)).isTrue();

    Map<String, String> map = new HashMap() {
      { put("foo", "foo"); }
    };
    assertFalse(isEmpty(map));
    assertThat(isNotEmpty(map)).isTrue();

    ImplementsCollection<String> collection = new ImplementsCollection(false);
    assertFalse(isEmpty(collection));
    assertThat(isNotEmpty(collection)).isTrue();

    ImplementsMap<String, String> implMap = new ImplementsMap<>(false);
    assertFalse(isEmpty(implMap));
    assertThat(isNotEmpty(implMap)).isTrue();

    Object[] objects = new Object[1];
    assertFalse(isEmpty(objects));
    assertThat(isNotEmpty(objects)).isTrue();

    long[] longs = new long[1];
    assertFalse(isEmpty(longs));
    assertThat(isNotEmpty(longs)).isTrue();

    int[] ints = new int[1];
    assertFalse(isEmpty(ints));
    assertThat(isNotEmpty(ints)).isTrue();

    short[] shorts = new short[1];
    assertFalse(isEmpty(shorts));
    assertThat(isNotEmpty(shorts)).isTrue();

    char[] chars = new char[1];
    assertFalse(isEmpty(chars));
    assertThat(isNotEmpty(chars)).isTrue();

    byte[] bytes = new byte[1];
    assertFalse(isEmpty(bytes));
    assertThat(isNotEmpty(bytes)).isTrue();

    double[] doubles = new double[1];
    assertFalse(isEmpty(doubles));
    assertThat(isNotEmpty(doubles)).isTrue();

    float[] floats = new float[1];
    assertFalse(isEmpty(floats));
    assertThat(isNotEmpty(floats)).isTrue();

    boolean[] booleans = new boolean[1];
    assertFalse(isEmpty(booleans));
    assertThat(isNotEmpty(booleans)).isTrue();
  }
}
