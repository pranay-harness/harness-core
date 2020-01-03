package io.harness.health;

import static io.harness.rule.OwnerRule.GEORGE;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.threading.Morpheus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HealthServiceTest extends CategoryTest {
  private static final Duration DEFAULT_VALID_FOR = Duration.ofMillis(110);
  private static final Duration DEFAULT_EXPECTED_RESPONSE_TIMEOUT = Duration.ofMillis(100);

  private ExecutorService executorService = Executors.newFixedThreadPool(25);

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEmpty() throws Exception {
    final HealthService healthService = new HealthService(executorService);
    assertThat(healthService.check().isHealthy()).isTrue();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSimpleMonitor() throws Exception {
    final HealthMonitor mock = mock(HealthMonitor.class);
    final HealthService healthService = new HealthService(executorService);
    healthService.registerMonitor(mock);

    when(mock.healthValidFor()).thenReturn(DEFAULT_VALID_FOR);
    when(mock.healthExpectedResponseTimeout()).thenReturn(DEFAULT_EXPECTED_RESPONSE_TIMEOUT);

    assertThat(healthService.check().isHealthy()).isTrue();

    verify(mock).isHealthy();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testRateLimited() throws Exception {
    final HealthMonitor mock = mock(HealthMonitor.class);
    final HealthService healthService = new HealthService(executorService);
    healthService.registerMonitor(mock);

    when(mock.healthValidFor()).thenReturn(ofMillis(200));
    when(mock.healthExpectedResponseTimeout()).thenReturn(ofMillis(100));

    assertThat(healthService.check().isHealthy()).isTrue();
    assertThat(healthService.check().isHealthy()).isTrue();
    assertThat(healthService.check().isHealthy()).isTrue();

    verify(mock, times(1)).isHealthy();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testUpdate() throws Exception {
    final HealthMonitor mock = mock(HealthMonitor.class);
    final HealthService healthService = new HealthService(executorService);
    healthService.registerMonitor(mock);

    when(mock.healthValidFor()).thenReturn(DEFAULT_VALID_FOR);
    when(mock.healthExpectedResponseTimeout()).thenReturn(DEFAULT_EXPECTED_RESPONSE_TIMEOUT);

    assertThat(healthService.check().isHealthy()).isTrue();
    Morpheus.sleep(ofMillis(DEFAULT_VALID_FOR.toMillis() + 1));
    assertThat(healthService.check().isHealthy()).isTrue();

    verify(mock, times(2)).isHealthy();
  }
}
