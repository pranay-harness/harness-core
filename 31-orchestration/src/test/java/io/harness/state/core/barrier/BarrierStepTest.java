package io.harness.state.core.barrier;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.rule.Owner;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;

public class BarrierStepTest extends OrchestrationTest {
  @Inject BarrierStep barrierStep;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestExecuteSync() {
    Ambiance ambiance = Ambiance.builder().build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    BarrierStepParameters stepParameters =
        BarrierStepParameters.builder().identifier("someString").timeoutInMillis(1000).build();
    StepResponse stepResponse = barrierStep.executeSync(ambiance, stepParameters, stepInputPackage, null);
    assertThat(stepResponse).isNotNull();
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestExecuteAsync() {
    Ambiance ambiance = Ambiance.builder().build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    BarrierStepParameters stepParameters = BarrierStepParameters.builder().identifier("someString").build();
    AsyncExecutableResponse stepResponse = barrierStep.executeAsync(ambiance, stepParameters, stepInputPackage);
    assertThat(stepResponse).isNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldHandleAsyncResponse() {
    Ambiance ambiance = Ambiance.builder().build();
    BarrierStepParameters stepParameters = BarrierStepParameters.builder().identifier("someString").build();
    StepResponse stepResponse = barrierStep.handleAsyncResponse(ambiance, stepParameters, new HashMap<>());
    assertThat(stepResponse).isNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldHandleAbort() {
    Ambiance ambiance = Ambiance.builder().build();
    BarrierStepParameters stepParameters = BarrierStepParameters.builder().identifier("someString").build();
    assertThatThrownBy(
        () -> barrierStep.handleAbort(ambiance, stepParameters, AsyncExecutableResponse.builder().build()))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
