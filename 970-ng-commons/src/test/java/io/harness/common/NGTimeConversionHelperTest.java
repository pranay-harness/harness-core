/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.common;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NGTimeConversionHelperTest extends CategoryTest {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testTimeInMilliseconds() {
    String time = "10w2d3h2m5ms";
    long result = io.harness.common.NGTimeConversionHelper.convertTimeStringToMilliseconds(time);
    assertThat(result).isEqualTo(6231720005L);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testTimeInMillisecondsWith0d() {
    String time = "10w0d2h";
    long result = io.harness.common.NGTimeConversionHelper.convertTimeStringToMilliseconds(time);
    assertThat(result).isEqualTo(6055200000L);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testTimeInMillisecondsWithOrderMismatch() {
    String time = "2d10w2m5ms3h";
    long result = io.harness.common.NGTimeConversionHelper.convertTimeStringToMilliseconds(time);
    assertThat(result).isEqualTo(6231720005L);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testTimeInMilliSecondsIncorrectTimeString() {
    String time = "2fdd10w2m5ms3h";
    assertThatThrownBy(() -> NGTimeConversionHelper.convertTimeStringToMilliseconds(time))
        .isInstanceOf(InvalidRequestException.class);
  }
}
