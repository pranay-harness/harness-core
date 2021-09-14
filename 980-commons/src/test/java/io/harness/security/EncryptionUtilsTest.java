/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.base.Charsets;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EncryptionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = OwnerRule.UTKARSH)
  @Category(UnitTests.class)
  public void toBytes() {
    final char[] src = "input-array".toCharArray();
    final byte[] bytes = EncryptionUtils.toBytes(src, Charsets.UTF_8);
    assertThat(String.valueOf(src)).isEqualTo("input-array");
  }
}
