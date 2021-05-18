package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.health.HealthException;
import io.harness.health.HealthService;
import io.harness.maintenance.MaintenanceController;
import io.harness.rule.Owner;

import com.codahale.metrics.health.HealthCheck;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MaintenanceController.class)
@OwnedBy(CE)
public class HealthResourceTest extends CategoryTest {
  private HealthResource healthResource;
  private HealthService healthService;

  @Before
  public void setup() {
    healthService = mock(HealthService.class);
    healthResource = new HealthResource(healthService);
    mockStatic(MaintenanceController.class);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGet_success() throws Exception {
    when(MaintenanceController.getMaintenanceFlag()).thenReturn(false);
    when(healthService.check()).thenReturn(HealthCheck.Result.healthy());

    String healthResponse = healthResource.get().getData();
    assertThat(healthResponse).isNotNull();
    assertThat(healthResponse).isEqualTo(HealthService.HEALTHY);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGet_failure_due_to_monitor() throws Exception {
    when(healthService.check()).thenReturn(HealthCheck.Result.unhealthy("DB down"));

    assertThatThrownBy(() -> healthResource.get().getData()).isExactlyInstanceOf(HealthException.class);
  }
}