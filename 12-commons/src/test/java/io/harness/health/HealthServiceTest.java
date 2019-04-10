package io.harness.health;

import static java.time.Duration.ofMillis;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.threading.Morpheus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HealthServiceTest {
  private static final Duration DEFAULT_VALID_FOR = Duration.ofMillis(110);
  private static final Duration DEFAULT_EXPECTED_RESPONSE_TIMEOUT = Duration.ofMillis(100);

  private ExecutorService executorService = Executors.newFixedThreadPool(25);

  @Test
  @Category(UnitTests.class)
  public void testEmpty() throws Exception {
    final HealthService healthService = new HealthService(executorService);
    assertTrue(healthService.check().isHealthy());
  }

  @Test
  @Category(UnitTests.class)
  public void testSimpleMonitor() throws Exception {
    final HealthMonitor mock = mock(HealthMonitor.class);
    final HealthService healthService = new HealthService(executorService);
    healthService.registerMonitor(mock);

    when(mock.healthValidFor()).thenReturn(DEFAULT_VALID_FOR);
    when(mock.healthExpectedResponseTimeout()).thenReturn(DEFAULT_EXPECTED_RESPONSE_TIMEOUT);

    assertTrue(healthService.check().isHealthy());

    verify(mock).isHealthy();
  }

  @Test
  @Category(UnitTests.class)
  public void testRateLimited() throws Exception {
    final HealthMonitor mock = mock(HealthMonitor.class);
    final HealthService healthService = new HealthService(executorService);
    healthService.registerMonitor(mock);

    when(mock.healthValidFor()).thenReturn(ofMillis(200));
    when(mock.healthExpectedResponseTimeout()).thenReturn(ofMillis(100));

    assertTrue(healthService.check().isHealthy());
    assertTrue(healthService.check().isHealthy());
    assertTrue(healthService.check().isHealthy());

    verify(mock, times(1)).isHealthy();
  }

  @Test
  @Category(UnitTests.class)
  public void testUpdate() throws Exception {
    final HealthMonitor mock = mock(HealthMonitor.class);
    final HealthService healthService = new HealthService(executorService);
    healthService.registerMonitor(mock);

    when(mock.healthValidFor()).thenReturn(DEFAULT_VALID_FOR);
    when(mock.healthExpectedResponseTimeout()).thenReturn(DEFAULT_EXPECTED_RESPONSE_TIMEOUT);

    assertTrue(healthService.check().isHealthy());
    Morpheus.sleep(ofMillis(DEFAULT_VALID_FOR.toMillis() + 1));
    assertTrue(healthService.check().isHealthy());

    verify(mock, times(2)).isHealthy();
  }
}
