/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.data.structure;

import static io.harness.data.structure.SizeFunction.size;
import static io.harness.rule.OwnerRule.GEORGE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.SizeFunction.Size;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SizePredicateTest extends CategoryTest {
  @Builder
  static class Custom implements Size {
    private int size;

    @Override
    public int size() {
      return size;
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNull() {
    Custom custom = null;
    assertThat(size(custom)).isEqualTo(0);

    List<Integer> list = null;
    assertThat(size(list)).isEqualTo(0);

    String string = null;
    assertThat(size(string)).isEqualTo(0);

    Map map = null;
    assertThat(size(map)).isEqualTo(0);

    ImplementsCollection<String> collection = null;
    assertThat(size(collection)).isEqualTo(0);

    ImplementsMap<String, String> implMap = null;
    assertThat(size(implMap)).isEqualTo(0);

    Object[] objects = null;
    assertThat(size(objects)).isEqualTo(0);

    long[] longs = null;
    assertThat(size(longs)).isEqualTo(0);

    int[] ints = null;
    assertThat(size(ints)).isEqualTo(0);

    short[] shorts = null;
    assertThat(size(shorts)).isEqualTo(0);

    char[] chars = null;
    assertThat(size(chars)).isEqualTo(0);

    byte[] bytes = null;
    assertThat(size(bytes)).isEqualTo(0);

    double[] doubles = null;
    assertThat(size(doubles)).isEqualTo(0);

    float[] floats = null;
    assertThat(size(floats)).isEqualTo(0);

    boolean[] booleans = null;
    assertThat(size(booleans)).isEqualTo(0);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEmpty() {
    Custom custom = Custom.builder().size(0).build();
    assertThat(size(custom)).isEqualTo(0);

    List<String> list = new ArrayList();
    assertThat(size(list)).isEqualTo(0);

    String string = "";
    assertThat(size(string)).isEqualTo(0);

    Map map = new HashMap();
    assertThat(size(map)).isEqualTo(0);

    ImplementsCollection<String> collection = new ImplementsCollection(true);
    assertThat(size(collection)).isEqualTo(0);

    ImplementsMap<String, String> implMap = new ImplementsMap<>(true);
    assertThat(size(implMap)).isEqualTo(0);

    Object[] objects = new Object[0];
    assertThat(size(objects)).isEqualTo(0);

    long[] longs = new long[0];
    assertThat(size(longs)).isEqualTo(0);

    int[] ints = new int[0];
    assertThat(size(ints)).isEqualTo(0);

    short[] shorts = new short[0];
    assertThat(size(shorts)).isEqualTo(0);

    char[] chars = new char[0];
    assertThat(size(chars)).isEqualTo(0);

    byte[] bytes = new byte[0];
    assertThat(size(bytes)).isEqualTo(0);

    double[] doubles = new double[0];
    assertThat(size(doubles)).isEqualTo(0);

    float[] floats = new float[0];
    assertThat(size(floats)).isEqualTo(0);

    boolean[] booleans = new boolean[0];
    assertThat(size(booleans)).isEqualTo(0);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNotEmpty() {
    Custom custom = Custom.builder().size(1).build();
    assertThat(size(custom)).isEqualTo(1);

    List<String> list = asList("foo");
    assertThat(size(list)).isEqualTo(1);

    String string = "1";
    assertThat(size(string)).isEqualTo(1);

    Map<String, String> map = new HashMap() {
      { put("foo", "foo"); }
    };
    assertThat(size(map)).isEqualTo(1);

    ImplementsCollection<String> collection = new ImplementsCollection(false);
    assertThat(size(collection)).isEqualTo(1);

    ImplementsMap<String, String> implMap = new ImplementsMap<>(false);
    assertThat(size(implMap)).isEqualTo(1);

    Object[] objects = new Object[1];
    assertThat(size(objects)).isEqualTo(1);

    long[] longs = new long[1];
    assertThat(size(longs)).isEqualTo(1);

    int[] ints = new int[1];
    assertThat(size(ints)).isEqualTo(1);

    short[] shorts = new short[1];
    assertThat(size(shorts)).isEqualTo(1);

    char[] chars = new char[1];
    assertThat(size(chars)).isEqualTo(1);

    byte[] bytes = new byte[1];
    assertThat(size(bytes)).isEqualTo(1);

    double[] doubles = new double[1];
    assertThat(size(doubles)).isEqualTo(1);

    float[] floats = new float[1];
    assertThat(size(floats)).isEqualTo(1);

    boolean[] booleans = new boolean[1];
    assertThat(size(booleans)).isEqualTo(1);
  }
}
