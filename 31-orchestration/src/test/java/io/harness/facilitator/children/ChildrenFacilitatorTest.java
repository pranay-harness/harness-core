package io.harness.facilitator.children;

import static io.harness.pms.execution.ExecutionMode.CHILDREN;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTestBase;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;

public class ChildrenFacilitatorTest extends OrchestrationTestBase {
  @Inject private ChildrenFacilitator childrenFacilitator;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFacilitate() {
    Ambiance ambiance = Ambiance.builder().build();
    FacilitatorParameters parameters = DefaultFacilitatorParams.builder().build();
    FacilitatorResponse response = childrenFacilitator.facilitate(ambiance, null, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(CHILDREN);
    assertThat(response.getInitialWait()).isEqualTo(Duration.ofSeconds(0));
  }
}