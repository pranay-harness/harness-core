/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import static io.harness.rule.OwnerRule.VARDAN_BANSAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.time.CalendarUtils;

import java.util.Calendar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CalendarUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void testGetCalendarForTimeZone() {
    Calendar calendar = CalendarUtils.getCalendarForTimeZone("Asia/Kolkata");
    assertThat(calendar.getTimeZone().getID()).isEqualTo("Asia/Kolkata");
  }
}
