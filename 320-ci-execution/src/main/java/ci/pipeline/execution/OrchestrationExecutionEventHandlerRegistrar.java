package ci.pipeline.execution;

import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_END;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.pipeline.executions.CIPipelineEndEventHandler;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class OrchestrationExecutionEventHandlerRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> handlerMap = new HashMap<>();
    handlerMap.put(NODE_EXECUTION_STATUS_UPDATE, Sets.newHashSet(PipelineExecutionUpdateEventHandler.class));
    handlerMap.put(ORCHESTRATION_END, Sets.newHashSet(CIPipelineEndEventHandler.class));
    return handlerMap;
  }
}
