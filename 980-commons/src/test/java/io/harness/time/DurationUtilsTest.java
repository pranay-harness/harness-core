/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.time;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.time.DurationUtils.durationTillDayTime;
import static io.harness.time.DurationUtils.truncate;

import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DurationUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testTruncate() throws Exception {
    Duration duration = ofDays(7).plusHours(3).plusSeconds(33).plusMillis(23);
    assertThat(truncate(duration, ofDays(1))).isEqualTo(ofDays(7));
    assertThat(truncate(duration, ofDays(2))).isEqualTo(ofDays(6));
    assertThat(truncate(duration, ofHours(2))).isEqualTo(ofDays(7).plusHours(2));
    assertThat(truncate(duration, ofHours(1))).isEqualTo(ofDays(7).plusHours(3));
    assertThat(truncate(duration, Duration.ofSeconds(10))).isEqualTo(ofDays(7).plusHours(3).plusSeconds(30));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testDurationTill() {
    assertThat(durationTillDayTime(1606003200000L, ofHours(3))).hasMillis(ofHours(3).toMillis());
    assertThat(durationTillDayTime(1606010400000L, ofHours(3))).hasMillis(ofHours(1).toMillis());
    assertThat(durationTillDayTime(1606014000000L, ofHours(3))).hasMillis(0);
    assertThat(durationTillDayTime(1606053600000L, ofHours(3))).hasMillis(ofHours(13).toMillis());
  }
}
