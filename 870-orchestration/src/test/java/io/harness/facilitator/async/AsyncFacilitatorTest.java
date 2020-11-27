package io.harness.facilitator.async;

import static io.harness.pms.execution.ExecutionMode.ASYNC;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.pms.ambiance.Ambiance;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AsyncFacilitatorTest extends OrchestrationTestBase {
  @Inject private AsyncFacilitator asyncFacilitator;
  @Inject private KryoSerializer kryoSerializer;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFacilitate() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    byte[] parameters = kryoSerializer.asBytes(DefaultFacilitatorParams.builder().build());
    FacilitatorResponse response = asyncFacilitator.facilitate(ambiance, null, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(ASYNC);
    assertThat(response.getInitialWait()).isEqualTo(Duration.ofSeconds(0));
  }
}
