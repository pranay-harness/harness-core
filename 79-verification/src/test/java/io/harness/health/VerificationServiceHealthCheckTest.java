package io.harness.health;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Pranjal on 09/24/2018
 */
public class VerificationServiceHealthCheckTest {
  public VerificationServiceHealthCheck healthCheck;

  @Before
  public void setUp() {
    healthCheck = new VerificationServiceHealthCheck();
  }

  @Test
  public void test_check_shouldSucess() {
    String expectedResult = "Verification Service Started";

    Result result = healthCheck.check();
    assertEquals(result.getMessage(), expectedResult);
  }
}
