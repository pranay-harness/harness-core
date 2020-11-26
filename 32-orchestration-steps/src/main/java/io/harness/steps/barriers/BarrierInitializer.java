package io.harness.steps.barriers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.distribution.barrier.Barrier;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.SyncOrchestrationEventHandler;
import io.harness.serializer.json.JsonOrchestrationUtils;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.service.BarrierService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BarrierInitializer implements SyncOrchestrationEventHandler {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private BarrierService barrierService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    String planExecutionId = event.getAmbiance().getPlanExecutionId();
    PlanExecution planExecution = Preconditions.checkNotNull(planExecutionService.get(planExecutionId));
    Map<String, List<BarrierExecutionInstance>> barrierIdentifierMap =
        planExecution.getPlan()
            .getNodes()
            .stream()
            .filter(planNode -> planNode.getStepType().equals(BarrierStep.STEP_TYPE))
            .map(planNode -> {
              BarrierStepParameters stepParameters =
                  JsonOrchestrationUtils.asObject(planNode.getStepParameters().toJson(), BarrierStepParameters.class);
              return BarrierExecutionInstance.builder()
                  .uuid(generateUuid())
                  .name(planNode.getName())
                  .planNodeId(planNode.getUuid())
                  .identifier(stepParameters.getIdentifier())
                  .planExecutionId(planExecution.getUuid())
                  .barrierState(Barrier.State.STANDING)
                  .expiredIn(stepParameters.getTimeoutInMillis())
                  .build();
            })
            .collect(Collectors.groupingBy(BarrierExecutionInstance::getIdentifier));

    // for each instance with the same identifier we set the barrierGroupId, which will be used with Wait Notify Engine
    barrierIdentifierMap.forEach((key, value) -> {
      String barrierGroupId = generateUuid();
      value.forEach(barrierExecutionInstance -> barrierExecutionInstance.setBarrierGroupId(barrierGroupId));
    });

    List<BarrierExecutionInstance> barrierExecutionInstances =
        barrierIdentifierMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

    barrierService.saveAll(barrierExecutionInstances);
  }
}
