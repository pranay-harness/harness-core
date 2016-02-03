package software.wings.health;

import com.codahale.metrics.health.HealthCheck;

import software.wings.app.MainConfiguration;

/**
 *  HealthCheck class for the Wings Application
 *
 *
 * @author Rishi
 *
 */
public class WingsHealthCheck extends HealthCheck {
  private MainConfiguration configuration;

  /**
   * @param configuration
   */
  public WingsHealthCheck(MainConfiguration configuration) {
    this.configuration = configuration;
  }

  /* (non-Javadoc)
   * @see com.codahale.metrics.health.HealthCheck#check()
   */
  @Override
  protected Result check() throws Exception {
    return Result.healthy("WingsApp Started");
  }
}
