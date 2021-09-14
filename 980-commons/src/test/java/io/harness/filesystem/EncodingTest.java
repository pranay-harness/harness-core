/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.filesystem;

import static io.harness.rule.OwnerRule.BRETT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import java.nio.charset.Charset;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EncodingTest extends CategoryTest {
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testDefaultEncoding() {
    assertThat(Charset.defaultCharset()).isEqualTo(Charsets.UTF_8);
  }
}
