package io.harness.facilitator.child;

import static io.harness.pms.execution.ExecutionMode.CHILD;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTestBase;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;

public class ChildFacilitatorTest extends OrchestrationTestBase {
  @Inject private ChildFacilitator childFacilitator;
  @Inject private KryoSerializer kryoSerializer;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFacilitate() {
    Ambiance ambiance = Ambiance.builder().build();
    byte[] parameters = kryoSerializer.asBytes(DefaultFacilitatorParams.builder().build());
    FacilitatorResponse response = childFacilitator.facilitate(ambiance, null, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(CHILD);
    assertThat(response.getInitialWait()).isEqualTo(Duration.ofSeconds(0));
  }
}
