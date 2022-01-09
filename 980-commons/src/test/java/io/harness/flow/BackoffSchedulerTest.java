/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.flow;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BackoffSchedulerTest extends CategoryTest {
  private static final String SERVICE = "service";
  private static final Duration MIN_DELAY = Duration.ofSeconds(1);
  private static final Duration MAX_DELAY = Duration.ofSeconds(10);

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldChangeDelayForSuccessAndFailure() throws Exception {
    BackoffScheduler scheduler = new BackoffScheduler(SERVICE, MIN_DELAY, MAX_DELAY);
    assertThat(scheduler.getCurrentDelayMs()).isEqualTo(1000L);
    scheduler.recordFailure();
    assertThat(scheduler.getCurrentDelayMs()).isEqualTo(2000L);
    scheduler.recordFailure();
    assertThat(scheduler.getCurrentDelayMs()).isEqualTo(4000L);
    scheduler.recordSuccess();
    assertThat(scheduler.getCurrentDelayMs()).isEqualTo(2000L);
    scheduler.recordSuccess();
    assertThat(scheduler.getCurrentDelayMs()).isEqualTo(1000L);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldDelayBeAtMostMaxDelay() throws Exception {
    BackoffScheduler scheduler = new BackoffScheduler(SERVICE, MIN_DELAY, MAX_DELAY);
    for (int i = 0; i < 5; i++) {
      scheduler.recordFailure();
    }
    assertThat(scheduler.getCurrentDelayMs()).isEqualTo(MAX_DELAY.toMillis());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldDelayBeAtLeastMinDelay() throws Exception {
    BackoffScheduler scheduler = new BackoffScheduler(SERVICE, MIN_DELAY, MAX_DELAY);
    scheduler.recordFailure();
    for (int i = 0; i < 5; i++) {
      scheduler.recordSuccess();
    }
    assertThat(scheduler.getCurrentDelayMs()).isEqualTo(MIN_DELAY.toMillis());
  }
}
