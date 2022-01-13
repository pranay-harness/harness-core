/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.time;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FakeClockTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testAdvanceBy() throws Exception {
    FakeClock fake = new FakeClock();
    Clock fixed = fake.toFixed();

    fake.advanceBy(1, ChronoUnit.MILLIS);
    assertThat(fake.getZone()).isEqualTo(fixed.getZone());
    assertThat(fake.toFixed()).isEqualTo(Clock.fixed(fixed.instant().plus(1, ChronoUnit.MILLIS), ZoneId.of("UTC")));
    assertThat(Duration.between(fixed.instant(), fake.instant())).isEqualTo(Duration.ofMillis(1));

    fake.advanceBy(99, ChronoUnit.MILLIS);
    assertThat(fake.getZone()).isEqualTo(fixed.getZone());
    assertThat(Duration.between(fixed.instant(), fake.instant())).isEqualTo(Duration.ofMillis(100));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testSetInstant() throws Exception {
    FakeClock fake = new FakeClock();
    Clock fixed = fake.toFixed();

    fake.instant(fixed.instant().plus(2, ChronoUnit.MINUTES));
    assertThat(fake.getZone()).isEqualTo(fixed.getZone());
    assertThat(Duration.between(fixed.instant(), fake.instant())).isEqualTo(Duration.ofMinutes(2));
  }
}
