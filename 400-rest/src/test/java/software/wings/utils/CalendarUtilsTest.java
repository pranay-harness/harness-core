/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
