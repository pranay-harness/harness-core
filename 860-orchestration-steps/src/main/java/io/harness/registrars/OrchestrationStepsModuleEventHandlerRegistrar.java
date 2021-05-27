package io.harness.registrars;

import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.steps.barriers.BarrierInitializer;
import io.harness.steps.barriers.event.BarrierDropper;
import io.harness.steps.barriers.event.BarrierPositionHelperEventHandler;
import io.harness.steps.resourcerestraint.ResourceRestraintInitializer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class OrchestrationStepsModuleEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    return ImmutableMap.<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>>builder()
        .put(ORCHESTRATION_START, Sets.newHashSet(BarrierInitializer.class, ResourceRestraintInitializer.class))
        .put(NODE_EXECUTION_STATUS_UPDATE,
            Sets.newHashSet(BarrierPositionHelperEventHandler.class, BarrierDropper.class))
        .build();
  }
}
