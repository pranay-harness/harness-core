package io.harness.grpc.utils;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.protobuf.Duration;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.DataFormatException;
import io.harness.rule.Owner;
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
    assertThatExceptionOfType(DataFormatException.class).isThrownBy(() -> HDurations.parse("random"));
  }
}
