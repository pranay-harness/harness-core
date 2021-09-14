/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.structure;

import static io.harness.rule.OwnerRule.SRINIVAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MapUtilTest extends CategoryTest {
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldPutIfNotEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtils.putIfNotEmpty("key", "value", input);
    assertThat(input.get("key")).isNotNull();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotPutIfEmpty() {
    Map<String, String> input = new HashMap<>();
    MapUtils.putIfNotEmpty("key", "", input);
    assertThat(input.get("key")).isNull();
  }
}
