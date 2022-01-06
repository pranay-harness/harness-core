/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.service;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.resources.stats.model.TimeRange;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TimeRangeProviderTest extends CategoryTest {
  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void monthlyRanges() {
    LocalDateTime from = LocalDateTime.parse("2018-10-03T10:15:30");
    LocalDateTime to = LocalDateTime.parse("2019-01-03T10:15:30");

    TimeRangeProvider provider = new TimeRangeProvider(ZoneOffset.UTC);
    List<TimeRange> timeRanges = provider.monthlyRanges(from.toInstant(ZoneOffset.UTC), to.toInstant(ZoneOffset.UTC));
    assertThat(timeRanges).hasSize(4);
    assertThat(timeRanges.get(0).getLabel()).isEqualTo("October 2018");
    assertThat(timeRanges.get(3).getLabel()).isEqualTo("January 2019");
  }
}
