package io.harness.delegate.app.health;

import io.harness.health.HealthMonitor;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelegateHealthMonitor implements HealthMonitor {
  @Override
  public Duration healthExpectedResponseTimeout() {
    return Duration.ofSeconds(1);
  }

  @Override
  public Duration healthValidFor() {
    return Duration.ofMinutes(5);
  }

  @Override
  public void isHealthy() {
    log.info("Delegate is healthy");
  }
}
