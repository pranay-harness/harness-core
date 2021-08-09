package io.harness.health;

import java.time.Duration;

public interface HealthMonitor {
  // For how long the health service should wait the monitor for response.
  Duration healthExpectedResponseTimeout();

  // For how long the healthy response is valid. It should include (be always bigger than) the expected response time.
  Duration healthValidFor();

  // Should throw an exception if the system is unhealthy.
  void isHealthy();
}
