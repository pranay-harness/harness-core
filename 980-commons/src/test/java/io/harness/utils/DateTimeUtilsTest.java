/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class DateTimeUtilsTest extends CategoryTest {
  private static final String UTC_TIMEZONE = "UTC";

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFormatDate() {
    assertThat(DateTimeUtils.formatDate(LocalDate.of(2000, 1, 1))).isEqualTo("2000-01-01");
    assertThat(DateTimeUtils.formatDate(ZonedDateTime.of(2000, 1, 1, 10, 10, 10, 10, ZoneId.of(UTC_TIMEZONE))))
        .isEqualTo("2000-01-01");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFormatDateTime() {
    assertThatThrownBy(() -> DateTimeUtils.formatDateTime(LocalDate.of(2000, 1, 1))).isNotNull();
    assertThat(DateTimeUtils.formatDateTime(ZonedDateTime.of(2000, 1, 1, 10, 10, 10, 10, ZoneId.of(UTC_TIMEZONE))))
        .isEqualTo("2000-01-01T10:10:10.000+0000");
  }
}
