package io.harness.steps.barriers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.distribution.barrier.Barrier;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.service.BarrierService;
import io.harness.timeout.TimeoutParameters;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BarrierInitializer implements SyncOrchestrationEventHandler {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private BarrierService barrierService;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    String planExecutionId = event.getAmbiance().getPlanExecutionId();
    PlanExecution planExecution = Preconditions.checkNotNull(planExecutionService.get(planExecutionId));
    Map<String, BarrierSetupInfo> barrierIdentifierSetupInfoMap =
        barrierService.getBarrierSetupInfoList(event.getAmbiance().getMetadata().getYaml())
            .stream()
            .collect(Collectors.toMap(BarrierSetupInfo::getIdentifier, Function.identity()));
    Map<String, List<BarrierExecutionInstance>> barrierIdentifierMap =
        planExecution.getPlan()
            .getNodes()
            .stream()
            .filter(planNode -> planNode.getStepType().equals(BarrierStep.STEP_TYPE))
            .map(planNode -> {
              BarrierStepParameters stepParameters = Objects.requireNonNull(
                  RecastOrchestrationUtils.fromDocumentJson(planNode.getStepParameters(), BarrierStepParameters.class));
              long expiredIn = planNode.getTimeoutObtainmentsList()
                                   .stream()
                                   .map(t
                                       -> ((TimeoutParameters) kryoSerializer.asObject(t.getParameters().toByteArray()))
                                              .getTimeoutMillis())
                                   .max(Long::compareTo)
                                   .orElse(TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS);
              return BarrierExecutionInstance.builder()
                  .uuid(generateUuid())
                  .name(planNode.getName())
                  .planNodeId(planNode.getUuid())
                  .identifier(stepParameters.getIdentifier())
                  .planExecutionId(planExecution.getUuid())
                  .barrierState(Barrier.State.STANDING)
                  .expiredIn(expiredIn)
                  .setupInfo(barrierIdentifierSetupInfoMap.get(stepParameters.getIdentifier()))
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
