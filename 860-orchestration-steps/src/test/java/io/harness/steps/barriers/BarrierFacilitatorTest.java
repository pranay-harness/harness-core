package io.harness.steps.barriers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.execution.ExecutionMode.ASYNC;
import static io.harness.pms.execution.ExecutionMode.SYNC;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationStepsTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.sdk.core.facilitator.DefaultFacilitatorParams;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.service.BarrierService;

import com.google.inject.Inject;
import io.fabric8.utils.Lists;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class BarrierFacilitatorTest extends OrchestrationStepsTestBase {
  @Mock private BarrierService barrierService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject @InjectMocks private BarrierFacilitator barrierFacilitator;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestFacilitateForSyncResponse() {
    when(barrierService.findByIdentifierAndPlanExecutionId(any(), any())).thenReturn(Collections.emptyList());
    Ambiance ambiance = Ambiance.newBuilder().build();
    byte[] parameters = kryoSerializer.asBytes(DefaultFacilitatorParams.builder().build());
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
    Ambiance ambiance = Ambiance.newBuilder().build();
    byte[] parameters = kryoSerializer.asBytes(DefaultFacilitatorParams.builder().build());
    BarrierStepParameters stepParameters = BarrierStepParameters.builder().identifier(identifier).build();
    FacilitatorResponse response = barrierFacilitator.facilitate(ambiance, stepParameters, parameters, null);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(ASYNC);
  }
}
