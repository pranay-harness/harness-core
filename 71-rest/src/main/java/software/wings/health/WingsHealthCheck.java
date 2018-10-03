package software.wings.health;

import com.codahale.metrics.health.HealthCheck;

/**
 * HealthCheck class for the Wings Application.
 *
 * @author Rishi
 */
public class WingsHealthCheck extends HealthCheck {
  /* (non-Javadoc)
   * @see com.codahale.metrics.health.HealthCheck#check()
   */
  @Override
  protected Result check() throws Exception {
    return Result.healthy("WingsApp Started");
  }
}
