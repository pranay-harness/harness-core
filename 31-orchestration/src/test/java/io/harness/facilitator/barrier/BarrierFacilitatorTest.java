package io.harness.facilitator.barrier;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.facilitator.modes.ExecutionMode.ASYNC;
import static io.harness.facilitator.modes.ExecutionMode.SYNC;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.fabric8.utils.Lists;
import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.barriers.BarrierExecutionInstance;
import io.harness.category.element.UnitTests;
import io.harness.engine.barriers.BarrierService;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.rule.Owner;
import io.harness.state.core.barrier.BarrierStepParameters;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

public class BarrierFacilitatorTest extends OrchestrationTest {
  @Mock private BarrierService barrierService;
  @Inject @InjectMocks private BarrierFacilitator barrierFacilitator;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestFacilitateForSyncResponse() {
    when(barrierService.findByIdentifierAndPlanExecutionId(any(), any())).thenReturn(Collections.emptyList());
    Ambiance ambiance = Ambiance.builder().build();
    FacilitatorParameters parameters = DefaultFacilitatorParams.builder().build();
    BarrierStepParameters stepParameters = BarrierStepParameters.builder().identifier("someString").build();
    FacilitatorResponse response = barrierFacilitator.facilitate(ambiance, stepParameters, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(SYNC);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestFacilitateForAsyncResponse() {
    String identifier = generateUuid();
    String planExecutionId = generateUuid();
    when(barrierService.findByIdentifierAndPlanExecutionId(any(), any()))
        .thenReturn(Lists.newArrayList(BarrierExecutionInstance.builder()
                                           .uuid(generateUuid())
                                           .identifier(identifier)
                                           .planExecutionId(planExecutionId)
                                           .build(),
            BarrierExecutionInstance.builder()
                .uuid(generateUuid())
                .identifier(identifier)
                .planExecutionId(planExecutionId)
                .build()));
    Ambiance ambiance = Ambiance.builder().build();
    FacilitatorParameters parameters = DefaultFacilitatorParams.builder().build();
    BarrierStepParameters stepParameters = BarrierStepParameters.builder().identifier(identifier).build();
    FacilitatorResponse response = barrierFacilitator.facilitate(ambiance, stepParameters, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(ASYNC);
  }
}
