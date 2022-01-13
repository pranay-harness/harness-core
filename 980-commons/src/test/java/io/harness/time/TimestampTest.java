/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.time;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class TimestampTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCurrentMinuteBoundary() {
    final long currentMinuteBoundary = Timestamp.currentMinuteBoundary();
    assertThat(currentMinuteBoundary).isBetween(currentMinuteBoundary, currentMinuteBoundary + 60 * 1000);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testMinuteBoundary() {
    assertThat(Timestamp.minuteBoundary(1524335288123L)).isEqualTo(1524335280000L);
  }
}
