package io.harness.steps.resourcerestraint;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.status.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationStepsTest;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.category.element.UnitTests;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.rule.Owner;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;

public class ResourceRestraintStepTest extends OrchestrationStepsTest {
  @Inject private ResourceRestraintStep resourceRestraintStep;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestExecuteAsync() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    String resourceConstraintId = generateUuid();
    String resourceUnit = generateUuid();
    Ambiance ambiance =
        Ambiance.builder()
            .levels(Collections.singletonList(Level.builder().runtimeId(uuid).setupId(planNodeId).build()))
            .build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceConstraintId(resourceConstraintId)
                                                         .resourceUnit(resourceUnit)
                                                         .build();

    AsyncExecutableResponse asyncExecutableResponse =
        resourceRestraintStep.executeAsync(ambiance, stepParameters, stepInputPackage);

    assertThat(asyncExecutableResponse).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestHandleAsyncResponse() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    String resourceConstraintId = generateUuid();
    String resourceUnit = generateUuid();
    Ambiance ambiance =
        Ambiance.builder()
            .levels(Collections.singletonList(Level.builder().runtimeId(uuid).setupId(planNodeId).build()))
            .build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceConstraintId(resourceConstraintId)
                                                         .resourceUnit(resourceUnit)
                                                         .build();

    StepResponse stepResponse = resourceRestraintStep.handleAsyncResponse(ambiance, stepParameters, null);

    assertThat(stepResponse).isNotNull();
    assertThat(stepResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldHandleAbort() {
    String uuid = generateUuid();
    String planNodeId = generateUuid();
    String resourceConstraintId = generateUuid();
    String resourceUnit = generateUuid();
    Ambiance ambiance =
        Ambiance.builder()
            .levels(Collections.singletonList(Level.builder().runtimeId(uuid).setupId(planNodeId).build()))
            .build();
    ResourceRestraintStepParameters stepParameters = ResourceRestraintStepParameters.builder()
                                                         .resourceConstraintId(resourceConstraintId)
                                                         .resourceUnit(resourceUnit)
                                                         .build();
    assertThatThrownBy(() -> resourceRestraintStep.handleAbort(ambiance, stepParameters, null))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
