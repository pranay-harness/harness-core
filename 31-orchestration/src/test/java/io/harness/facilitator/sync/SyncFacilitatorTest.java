package io.harness.facilitator.sync;

import static io.harness.facilitator.modes.ExecutionMode.SYNC;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.FacilitatorType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;

public class SyncFacilitatorTest extends OrchestrationTest {
  @Inject private SyncFacilitator syncFacilitator;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestGetType() {
    FacilitatorType facilitatorType = syncFacilitator.getType();
    assertThat(facilitatorType).isNotNull();
    assertThat(facilitatorType.getType()).isEqualTo(FacilitatorType.SYNC);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFacilitate() {
    Ambiance ambiance = Ambiance.builder().build();
    FacilitatorParameters parameters = DefaultFacilitatorParams.builder().build();
    FacilitatorResponse response = syncFacilitator.facilitate(ambiance, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(SYNC);
    assertThat(response.getInitialWait()).isEqualTo(Duration.ofSeconds(0));
  }
}