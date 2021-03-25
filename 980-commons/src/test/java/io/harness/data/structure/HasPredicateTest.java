package io.harness.data.structure;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.rule.OwnerRule.GEORGE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.HasPredicate.HasNone;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HasPredicateTest extends CategoryTest {
  @Getter
  @Builder
  static class Custom implements HasNone {
    boolean has;

    @Override
    public boolean hasNone() {
      return has;
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNull() {
    Custom custom = null;
    assertThat(hasNone(custom)).isTrue();
    assertThat(hasSome(custom)).isFalse();

    List<Integer> list = null;
    assertThat(hasNone(list)).isTrue();
    assertThat(hasSome(list)).isFalse();

    String string = null;
    assertThat(hasNone(string)).isTrue();
    assertThat(hasSome(string)).isFalse();

    Map map = null;
    assertThat(hasNone(map)).isTrue();
    assertThat(hasSome(map)).isFalse();

    ImplementsCollection<String> collection = null;
    assertThat(hasNone(collection)).isTrue();
    assertThat(hasSome(collection)).isFalse();

    ImplementsMap<String, String> implMap = null;
    assertThat(hasNone(implMap)).isTrue();
    assertThat(hasSome(implMap)).isFalse();

    Object[] objects = null;
    assertThat(hasNone(objects)).isTrue();
    assertThat(hasSome(objects)).isFalse();

    long[] longs = null;
    assertThat(hasNone(longs)).isTrue();
    assertThat(hasSome(longs)).isFalse();

    int[] ints = null;
    assertThat(hasNone(ints)).isTrue();
    assertThat(hasSome(ints)).isFalse();

    short[] shorts = null;
    assertThat(hasNone(shorts)).isTrue();
    assertThat(hasSome(shorts)).isFalse();

    char[] chars = null;
    assertThat(hasNone(chars)).isTrue();
    assertThat(hasSome(chars)).isFalse();

    byte[] bytes = null;
    assertThat(hasNone(bytes)).isTrue();
    assertThat(hasSome(bytes)).isFalse();

    double[] doubles = null;
    assertThat(hasNone(doubles)).isTrue();
    assertThat(hasSome(doubles)).isFalse();

    float[] floats = null;
    assertThat(hasNone(floats)).isTrue();
    assertThat(hasSome(floats)).isFalse();

    boolean[] booleans = null;
    assertThat(hasNone(booleans)).isTrue();
    assertThat(hasSome(booleans)).isFalse();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEmpty() {
    Custom custom = Custom.builder().has(true).build();
    assertThat(hasNone(custom)).isTrue();
    assertThat(hasSome(custom)).isFalse();

    List<String> list = new ArrayList();
    assertThat(hasNone(list)).isTrue();
    assertThat(hasSome(list)).isFalse();

    String string = "";
    assertThat(hasNone(string)).isTrue();
    assertThat(hasSome(string)).isFalse();

    Map<String, String> map = new HashMap();
    assertThat(hasNone(map)).isTrue();
    assertThat(hasSome(map)).isFalse();

    ImplementsCollection<String> collection = new ImplementsCollection(true);
    assertThat(hasNone(collection)).isTrue();
    assertThat(hasSome(collection)).isFalse();

    ImplementsMap<String, String> implMap = new ImplementsMap<>(true);
    assertThat(hasNone(implMap)).isTrue();
    assertThat(hasSome(implMap)).isFalse();

    Object[] objects = new Object[0];
    assertThat(hasNone(objects)).isTrue();
    assertThat(hasSome(objects)).isFalse();

    long[] longs = new long[0];
    assertThat(hasNone(longs)).isTrue();
    assertThat(hasSome(longs)).isFalse();

    int[] ints = new int[0];
    assertThat(hasNone(ints)).isTrue();
    assertThat(hasSome(ints)).isFalse();

    short[] shorts = new short[0];
    assertThat(hasNone(shorts)).isTrue();
    assertThat(hasSome(shorts)).isFalse();

    char[] chars = new char[0];
    assertThat(hasNone(chars)).isTrue();
    assertThat(hasSome(chars)).isFalse();

    byte[] bytes = new byte[0];
    assertThat(hasNone(bytes)).isTrue();
    assertThat(hasSome(bytes)).isFalse();

    double[] doubles = new double[0];
    assertThat(hasNone(doubles)).isTrue();
    assertThat(hasSome(doubles)).isFalse();

    float[] floats = new float[0];
    assertThat(hasNone(floats)).isTrue();
    assertThat(hasSome(floats)).isFalse();

    boolean[] booleans = new boolean[0];
    assertThat(hasNone(booleans)).isTrue();
    assertThat(hasSome(booleans)).isFalse();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNotEmpty() {
    Custom custom = Custom.builder().has(false).build();
    assertThat(hasNone(custom)).isFalse();
    assertThat(hasSome(custom)).isTrue();

    List<String> list = asList("foo");
    assertThat(hasNone(list)).isFalse();
    assertThat(hasSome(list)).isTrue();

    String string = "some";
    assertThat(hasNone(string)).isFalse();
    assertThat(hasSome(string)).isTrue();

    Map<String, String> map = new HashMap() {
      { put("foo", "foo"); }
    };
    assertThat(hasNone(map)).isFalse();
    assertThat(hasSome(map)).isTrue();

    ImplementsCollection<String> collection = new ImplementsCollection(false);
    assertThat(hasNone(collection)).isFalse();
    assertThat(hasSome(collection)).isTrue();

    ImplementsMap<String, String> implMap = new ImplementsMap<>(false);
    assertThat(hasNone(implMap)).isFalse();
    assertThat(hasSome(implMap)).isTrue();

    Object[] objects = new Object[1];
    assertThat(hasNone(objects)).isFalse();
    assertThat(hasSome(objects)).isTrue();

    long[] longs = new long[1];
    assertThat(hasNone(longs)).isFalse();
    assertThat(hasSome(longs)).isTrue();

    int[] ints = new int[1];
    assertThat(hasNone(ints)).isFalse();
    assertThat(hasSome(ints)).isTrue();

    short[] shorts = new short[1];
    assertThat(hasNone(shorts)).isFalse();
    assertThat(hasSome(shorts)).isTrue();

    char[] chars = new char[1];
    assertThat(hasNone(chars)).isFalse();
    assertThat(hasSome(chars)).isTrue();

    byte[] bytes = new byte[1];
    assertThat(hasNone(bytes)).isFalse();
    assertThat(hasSome(bytes)).isTrue();

    double[] doubles = new double[1];
    assertThat(hasNone(doubles)).isFalse();
    assertThat(hasSome(doubles)).isTrue();

    float[] floats = new float[1];
    assertThat(hasNone(floats)).isFalse();
    assertThat(hasSome(floats)).isTrue();

    boolean[] booleans = new boolean[1];
    assertThat(hasNone(booleans)).isFalse();
    assertThat(hasSome(booleans)).isTrue();
  }
}
