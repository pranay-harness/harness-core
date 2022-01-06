/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.threading;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.threading.Morpheus.sleep;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SchedulableTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSchedulable() {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("test-schedulable-%d").build());

    AtomicInteger nonSchedulableCount = new AtomicInteger(0);
    scheduledExecutorService.scheduleAtFixedRate(() -> {
      nonSchedulableCount.incrementAndGet();
      throw new OutOfMemoryError("");
    }, 0, 1, TimeUnit.MILLISECONDS);

    AtomicInteger schedulableCount = new AtomicInteger(0);
    scheduledExecutorService.scheduleAtFixedRate(new Schedulable(null, () -> {
      schedulableCount.incrementAndGet();
      throw new OutOfMemoryError("");
    }), 0, 1, TimeUnit.MILLISECONDS);

    sleep(Duration.ofMillis(100));

    scheduledExecutorService.shutdown();

    assertThat(schedulableCount.get()).isGreaterThan(1);
    assertThat(nonSchedulableCount.get()).isLessThanOrEqualTo(1);
  }
}
