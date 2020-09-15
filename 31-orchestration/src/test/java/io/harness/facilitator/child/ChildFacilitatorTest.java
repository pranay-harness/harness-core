package io.harness.facilitator.child;

import static io.harness.facilitator.modes.ExecutionMode.CHILD;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationModuleListProvider;
import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.rule.Owner;
import io.harness.runners.GuiceRunner;
import io.harness.runners.ModuleProvider;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.time.Duration;

@RunWith(GuiceRunner.class)
@ModuleProvider(OrchestrationModuleListProvider.class)
public class ChildFacilitatorTest extends OrchestrationTest {
  @Inject private ChildFacilitator childFacilitator;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFacilitate() {
    Ambiance ambiance = Ambiance.builder().build();
    FacilitatorParameters parameters = DefaultFacilitatorParams.builder().build();
    FacilitatorResponse response = childFacilitator.facilitate(ambiance, null, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(CHILD);
    assertThat(response.getInitialWait()).isEqualTo(Duration.ofSeconds(0));
  }
}