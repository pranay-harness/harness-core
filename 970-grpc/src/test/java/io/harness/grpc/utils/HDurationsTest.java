/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.utils;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HDurationsTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldParseDurationString() throws Exception {
    assertThat(HDurations.parse("42s")).isEqualTo(Duration.newBuilder().setSeconds(42).build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldThrowForInvalidDurationString() throws Exception {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HDurations.parse("random"))
        .withMessageContaining("Invalid format");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldParseMinute() throws Exception {
    assertThat(HDurations.parse("1m0s")).isEqualTo(Durations.fromMinutes(1));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldParseMinuteAndSecond() throws Exception {
    assertThat(HDurations.parse("1m23s")).isEqualTo(Durations.fromSeconds(83));
  }
}
