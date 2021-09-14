/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timescaledb;

import static io.harness.rule.OwnerRule.NANDAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.health.HealthService;
import io.harness.rule.Owner;
import io.harness.threading.ThreadPoolGuard;

import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TimeScaleDBServiceTest extends CategoryTest {
  private static final Duration DEFAULT_VALID_FOR = Duration.ofMillis(200);
  private static final Duration DEFAULT_EXPECTED_RESPONSE_TIMEOUT = Duration.ofMillis(100);

  @Test
  @Owner(developers = NANDAN)
  @Category(UnitTests.class)
  public void testIsHealthy() throws Exception {
    try (ThreadPoolGuard guard = new ThreadPoolGuard(Executors.newFixedThreadPool(2))) {
      final TimeScaleDBService timeScaleDBService = mock(TimeScaleDBService.class);
      final HealthService healthService = new HealthService(guard.getExecutorService());
      healthService.registerMonitor(timeScaleDBService);

      when(timeScaleDBService.healthValidFor()).thenReturn(DEFAULT_VALID_FOR);
      when(timeScaleDBService.healthExpectedResponseTimeout()).thenReturn(DEFAULT_EXPECTED_RESPONSE_TIMEOUT);

      assertThat(healthService.check().isHealthy()).isTrue();
      verify(timeScaleDBService).isHealthy();
    }
  }
}
