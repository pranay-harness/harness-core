package io.harness.health;

import com.codahale.metrics.health.HealthCheck;

/**
 * HealthCheck class for the Verification Service.
 *
 * @author Rishi
 */
public class VerificationServiceHealthCheck extends HealthCheck {
  /* (non-Javadoc)
   * @see com.codahale.metrics.health.HealthCheck#check()
   */
  @Override
  protected Result check() {
    return Result.healthy("Verification Service Started");
  }
}
