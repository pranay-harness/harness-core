/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.data.structure;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.GEORGE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.EmptyPredicate.IsEmpty;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EmptyPredicateTest extends CategoryTest {
  @Getter
  @Builder
  static class Custom implements IsEmpty {
    boolean empty;
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNull() {
    Custom custom = null;
    assertThat(isEmpty(custom)).isTrue();
    assertThat(isNotEmpty(custom)).isFalse();

    List<Integer> list = null;
    assertThat(isEmpty(list)).isTrue();
    assertThat(isNotEmpty(list)).isFalse();

    String string = null;
    assertThat(isEmpty(string)).isTrue();
    assertThat(isNotEmpty(string)).isFalse();

    Map map = null;
    assertThat(isEmpty(map)).isTrue();
    assertThat(isNotEmpty(map)).isFalse();

    ImplementsCollection<String> collection = null;
    assertThat(isEmpty(collection)).isTrue();
    assertThat(isNotEmpty(collection)).isFalse();

    ImplementsMap<String, String> implMap = null;
    assertThat(isEmpty(implMap)).isTrue();
    assertThat(isNotEmpty(implMap)).isFalse();

    Object[] objects = null;
    assertThat(isEmpty(objects)).isTrue();
    assertThat(isNotEmpty(objects)).isFalse();

    long[] longs = null;
    assertThat(isEmpty(longs)).isTrue();
    assertThat(isNotEmpty(longs)).isFalse();

    int[] ints = null;
    assertThat(isEmpty(ints)).isTrue();
    assertThat(isNotEmpty(ints)).isFalse();

    short[] shorts = null;
    assertThat(isEmpty(shorts)).isTrue();
    assertThat(isNotEmpty(shorts)).isFalse();

    char[] chars = null;
    assertThat(isEmpty(chars)).isTrue();
    assertThat(isNotEmpty(chars)).isFalse();

    byte[] bytes = null;
    assertThat(isEmpty(bytes)).isTrue();
    assertThat(isNotEmpty(bytes)).isFalse();

    double[] doubles = null;
    assertThat(isEmpty(doubles)).isTrue();
    assertThat(isNotEmpty(doubles)).isFalse();

    float[] floats = null;
    assertThat(isEmpty(floats)).isTrue();
    assertThat(isNotEmpty(floats)).isFalse();

    boolean[] booleans = null;
    assertThat(isEmpty(booleans)).isTrue();
    assertThat(isNotEmpty(booleans)).isFalse();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEmpty() {
    Custom custom = Custom.builder().empty(true).build();
    assertThat(isEmpty(custom)).isTrue();
    assertThat(isNotEmpty(custom)).isFalse();

    List<String> list = new ArrayList();
    assertThat(isEmpty(list)).isTrue();
    assertThat(isNotEmpty(list)).isFalse();

    String string = "";
    assertThat(isEmpty(string)).isTrue();
    assertThat(isNotEmpty(string)).isFalse();

    Map<String, String> map = new HashMap();
    assertThat(isEmpty(map)).isTrue();
    assertThat(isNotEmpty(map)).isFalse();

    ImplementsCollection<String> collection = new ImplementsCollection(true);
    assertThat(isEmpty(collection)).isTrue();
    assertThat(isNotEmpty(collection)).isFalse();

    ImplementsMap<String, String> implMap = new ImplementsMap<>(true);
    assertThat(isEmpty(implMap)).isTrue();
    assertThat(isNotEmpty(implMap)).isFalse();

    Object[] objects = new Object[0];
    assertThat(isEmpty(objects)).isTrue();
    assertThat(isNotEmpty(objects)).isFalse();

    long[] longs = new long[0];
    assertThat(isEmpty(longs)).isTrue();
    assertThat(isNotEmpty(longs)).isFalse();

    int[] ints = new int[0];
    assertThat(isEmpty(ints)).isTrue();
    assertThat(isNotEmpty(ints)).isFalse();

    short[] shorts = new short[0];
    assertThat(isEmpty(shorts)).isTrue();
    assertThat(isNotEmpty(shorts)).isFalse();

    char[] chars = new char[0];
    assertThat(isEmpty(chars)).isTrue();
    assertThat(isNotEmpty(chars)).isFalse();

    byte[] bytes = new byte[0];
    assertThat(isEmpty(bytes)).isTrue();
    assertThat(isNotEmpty(bytes)).isFalse();

    double[] doubles = new double[0];
    assertThat(isEmpty(doubles)).isTrue();
    assertThat(isNotEmpty(doubles)).isFalse();

    float[] floats = new float[0];
    assertThat(isEmpty(floats)).isTrue();
    assertThat(isNotEmpty(floats)).isFalse();

    boolean[] booleans = new boolean[0];
    assertThat(isEmpty(booleans)).isTrue();
    assertThat(isNotEmpty(booleans)).isFalse();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNotEmpty() {
    Custom custom = Custom.builder().empty(false).build();
    assertThat(isEmpty(custom)).isFalse();
    assertThat(isNotEmpty(custom)).isTrue();

    List<String> list = asList("foo");
    assertThat(isEmpty(list)).isFalse();
    assertThat(isNotEmpty(list)).isTrue();

    String string = "some";
    assertThat(isEmpty(string)).isFalse();
    assertThat(isNotEmpty(string)).isTrue();

    Map<String, String> map = new HashMap() {
      { put("foo", "foo"); }
    };
    assertThat(isEmpty(map)).isFalse();
    assertThat(isNotEmpty(map)).isTrue();

    ImplementsCollection<String> collection = new ImplementsCollection(false);
    assertThat(isEmpty(collection)).isFalse();
    assertThat(isNotEmpty(collection)).isTrue();

    ImplementsMap<String, String> implMap = new ImplementsMap<>(false);
    assertThat(isEmpty(implMap)).isFalse();
    assertThat(isNotEmpty(implMap)).isTrue();

    Object[] objects = new Object[1];
    assertThat(isEmpty(objects)).isFalse();
    assertThat(isNotEmpty(objects)).isTrue();

    long[] longs = new long[1];
    assertThat(isEmpty(longs)).isFalse();
    assertThat(isNotEmpty(longs)).isTrue();

    int[] ints = new int[1];
    assertThat(isEmpty(ints)).isFalse();
    assertThat(isNotEmpty(ints)).isTrue();

    short[] shorts = new short[1];
    assertThat(isEmpty(shorts)).isFalse();
    assertThat(isNotEmpty(shorts)).isTrue();

    char[] chars = new char[1];
    assertThat(isEmpty(chars)).isFalse();
    assertThat(isNotEmpty(chars)).isTrue();

    byte[] bytes = new byte[1];
    assertThat(isEmpty(bytes)).isFalse();
    assertThat(isNotEmpty(bytes)).isTrue();

    double[] doubles = new double[1];
    assertThat(isEmpty(doubles)).isFalse();
    assertThat(isNotEmpty(doubles)).isTrue();

    float[] floats = new float[1];
    assertThat(isEmpty(floats)).isFalse();
    assertThat(isNotEmpty(floats)).isTrue();

    boolean[] booleans = new boolean[1];
    assertThat(isEmpty(booleans)).isFalse();
    assertThat(isNotEmpty(booleans)).isTrue();
  }
}
