package io.harness.cvng.core.utils;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;

public class DateTimeUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void roundDownTo5MinBoundary() {
    assertThat(DateTimeUtils.roundDownTo5MinBoundary(Instant.parse("2020-04-22T10:02:06Z")))
        .isEqualTo(Instant.parse("2020-04-22T10:00:00Z"));
    assertThat(DateTimeUtils.roundDownTo5MinBoundary(Instant.parse("2020-04-22T10:00:06Z")))
        .isEqualTo(Instant.parse("2020-04-22T10:00:00Z"));
    assertThat(DateTimeUtils.roundDownTo5MinBoundary(Instant.parse("2020-04-22T10:59:59Z")))
        .isEqualTo(Instant.parse("2020-04-22T10:55:00Z"));
    assertThat(DateTimeUtils.roundDownTo5MinBoundary(Instant.parse("2020-04-22T10:15:59Z")))
        .isEqualTo(Instant.parse("2020-04-22T10:15:00Z"));
    assertThat(DateTimeUtils.roundDownTo5MinBoundary(Instant.parse("2020-04-22T10:26:00Z")))
        .isEqualTo(Instant.parse("2020-04-22T10:25:00Z"));
  }
}