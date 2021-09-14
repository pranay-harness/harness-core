/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.MDC;

public class AutoLogRemoveContextTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldRemove() {
    try (AutoLogContext add = new AutoLogContext("foo", "value", OVERRIDE_ERROR)) {
      assertThat(MDC.get("foo")).isEqualTo("value");
      try (AutoLogRemoveContext remove = new AutoLogRemoveContext("foo")) {
        assertThat(MDC.get("foo")).isNull();
      }
      assertThat(MDC.get("foo")).isEqualTo("value");
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldRemoveMoreThanOne() {
    try (AutoLogContext add1 = new AutoLogContext("foo", "value", OVERRIDE_ERROR);
         AutoLogContext add2 = new AutoLogContext("bar", "value", OVERRIDE_ERROR)) {
      assertThat(MDC.get("foo")).isEqualTo("value");
      assertThat(MDC.get("bar")).isEqualTo("value");
      try (AutoLogRemoveContext remove = new AutoLogRemoveContext("foo", "bar")) {
        assertThat(MDC.get("foo")).isNull();
        assertThat(MDC.get("bar")).isNull();
      }
      assertThat(MDC.get("foo")).isEqualTo("value");
      assertThat(MDC.get("bar")).isEqualTo("value");
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldDoNothingIfMissing() {
    try (AutoLogRemoveContext remove = new AutoLogRemoveContext("foo")) {
      assertThat(MDC.get("foo")).isNull();
    }
  }
}
