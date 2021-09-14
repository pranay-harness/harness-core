/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.structure;

import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HarnessStringUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldJoinIfIndividualElements() {
    String joinedString = HarnessStringUtils.join("/", "foo", "bar", "hello-world");
    assertThat(joinedString).isEqualTo("foo/bar/hello-world");

    joinedString = HarnessStringUtils.join(StringUtils.EMPTY, "foo", "bar", "hello-world");
    assertThat(joinedString).isEqualTo("foobarhello-world");
  }
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldJoinIfIterableElements() {
    String joinedString = HarnessStringUtils.join("/", Arrays.asList("foo", "bar", "hello-world"));
    assertThat(joinedString).isEqualTo("foo/bar/hello-world");

    joinedString = HarnessStringUtils.join(StringUtils.EMPTY,
        Arrays.asList("foo", "bar",
            "hello"
                + "-world"));
    assertThat(joinedString).isEqualTo("foobarhello-world");
  }
}
