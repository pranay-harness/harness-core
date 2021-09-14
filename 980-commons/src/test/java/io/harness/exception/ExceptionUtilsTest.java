/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExceptionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCause() {
    assertThat(ExceptionUtils.cause(Exception.class, null)).isNull();
    assertThat(ExceptionUtils.cause(Exception.class, new Exception())).isNotNull();

    assertThat(ExceptionUtils.cause(RuntimeException.class, new Exception())).isNull();
    assertThat(ExceptionUtils.cause(RuntimeException.class, new Exception(new RuntimeException()))).isNotNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCause_1() {
    final InvalidRequestException invalidRequestException =
        new InvalidRequestException("abcd", new InvalidTagException("xyz", null));
    assertThat(ExceptionUtils.cause(ErrorCode.IMAGE_TAG_NOT_FOUND, invalidRequestException)).isNotNull();
    assertThat(ExceptionUtils.cause(ErrorCode.SCM_NOT_FOUND_ERROR, invalidRequestException)).isNull();

    assertThat(ExceptionUtils.cause(ErrorCode.IMAGE_TAG_NOT_FOUND, new Exception())).isNull();
  }
}
