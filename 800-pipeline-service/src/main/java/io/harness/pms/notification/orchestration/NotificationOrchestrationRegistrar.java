package io.harness.pms.notification.orchestration;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_START;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.PLAN_EXECUTION_FAILED;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.PLAN_EXECUTION_PAUSED;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.PLAN_EXECUTION_SUCCESS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.notification.orchestration.handlers.PipelineFailedNotificationHandler;
import io.harness.pms.notification.orchestration.handlers.PipelinePausedNotificationHandler;
import io.harness.pms.notification.orchestration.handlers.PipelineStartNotificationHandler;
import io.harness.pms.notification.orchestration.handlers.PipelineSuccessNotificationHandler;
import io.harness.pms.notification.orchestration.handlers.StageStartNotificationHandler;
import io.harness.pms.notification.orchestration.handlers.StageStatusUpdateNotificationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class NotificationOrchestrationRegistrar {
  public Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> getEngineEventHandlers() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> engineEventHandlersMap =
        new HashMap<>();
    engineEventHandlersMap.put(PLAN_EXECUTION_SUCCESS, Sets.newHashSet(PipelineSuccessNotificationHandler.class));
    engineEventHandlersMap.put(PLAN_EXECUTION_PAUSED, Sets.newHashSet(PipelinePausedNotificationHandler.class));
    engineEventHandlersMap.put(PLAN_EXECUTION_FAILED, Sets.newHashSet(PipelineFailedNotificationHandler.class));
    engineEventHandlersMap.put(ORCHESTRATION_START, Sets.newHashSet(PipelineStartNotificationHandler.class));

    return engineEventHandlersMap;
  }
}
