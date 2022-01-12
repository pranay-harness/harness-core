/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.health;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

import io.harness.concurrent.HTimeLimiter;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.Builder;
import lombok.Value;

@Singleton
public class HealthService extends HealthCheck {
  public static final String HEALTHY = "healthy";
  boolean initial;
  TimeLimiter timeLimiter;
  ExecutorService executorService;

  @Inject
  public HealthService(ExecutorService executorService) {
    initial = true;
    timeLimiter = HTimeLimiter.create(executorService);
    this.executorService = executorService;
  }

  @Value
  @Builder
  static class Response {
    Future<Throwable> future;
    long validUntil;
  }

  private Map<HealthMonitor, Response> monitors = new HashMap<>();

  public void registerMonitor(HealthMonitor monitor) {
    synchronized (monitors) {
      monitors.put(monitor, Response.builder().build());
    }
  }

  public void unregisterMonitor(HealthMonitor monitor) {
    synchronized (monitors) {
      monitors.remove(monitor);
    }
  }

  @Override
  public Result check() throws Exception {
    long startTime = currentTimeMillis();

    // This seems as endless loop, but unless we adding monitors again and again this loop will exit
    // at the second iteration.
    while (true) {
      final Result result = checkMonitors(startTime);
      if (result != null) {
        if (result.isHealthy()) {
          initial = false;
        }
        return result;
      }
    }
  }

  @SuppressWarnings("PMD")
  private Throwable checkMonitor(HealthMonitor monitor) {
    try {
      Throwable exception = HTimeLimiter.callInterruptible21(
          timeLimiter, Duration.ofMillis(monitor.healthExpectedResponseTimeout().toMillis()), () -> {
            try {
              monitor.isHealthy();
              return null;
            } catch (Exception ex) {
              return ex;
            }
          });
      return exception;
    } catch (UncheckedTimeoutException exception) {
      return new HealthException(
          format("Monitor %s did not respond on time.", monitor.getClass().getName()), exception);
    } catch (Exception exception) {
      return exception;
    }
  }

  private void startCheck(HealthMonitor monitor) {
    final Future<Throwable> future = executorService.submit(() -> checkMonitor(monitor));
    monitors.put(monitor,
        Response.builder()
            .future(future)
            .validUntil(currentTimeMillis() + monitor.healthValidFor().toMillis())
            .build());
  }

  public Result checkMonitors(long startTime) {
    Result result = Result.healthy();

    synchronized (monitors) {
      for (Entry<HealthMonitor, Response> entity : monitors.entrySet()) {
        final HealthMonitor monitor = entity.getKey();
        Response response = entity.getValue();
        if (response.validUntil > startTime) {
          try {
            if (response.future.get() != null) {
              // if the system is in initial state, we would like to be checking more often
              if (initial) {
                startCheck(monitor);
              }
              return Result.unhealthy(response.future.get());
            }
          } catch (ExecutionException | InterruptedException exception) {
            return Result.unhealthy(exception);
          }
          continue;
        }

        result = null;
        startCheck(monitor);
      }
    }

    return result;
  }
}
