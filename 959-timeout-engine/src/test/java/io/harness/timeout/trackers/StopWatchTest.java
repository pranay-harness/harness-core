/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.timeout.trackers;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.TimeoutEngineTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StopWatchTest extends TimeoutEngineTestBase {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testTickingStopWatch() throws InterruptedException {
    StopWatch stopWatch = new StopWatch(true);
    Thread.sleep(5);
    assertThat(stopWatch.getElapsedMillis()).isGreaterThan(0);
    stopWatch.pause();
    long millis = stopWatch.getElapsedMillis();
    Thread.sleep(5);
    assertThat(stopWatch.getElapsedMillis()).isEqualTo(millis);
    stopWatch.resume();
    Thread.sleep(5);
    assertThat(stopWatch.getElapsedMillis()).isGreaterThan(millis);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testPausedStopWatch() throws InterruptedException {
    StopWatch stopWatch = new StopWatch(false);
    Thread.sleep(5);
    assertThat(stopWatch.getElapsedMillis()).isEqualTo(0);
    stopWatch.resume();
    Thread.sleep(5);
    assertThat(stopWatch.getElapsedMillis()).isGreaterThan(0);
  }
}
