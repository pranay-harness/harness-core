package io.harness.steps.barriers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.barrier.Barrier;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierPositionInfo;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.service.BarrierService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class BarrierInitializer implements SyncOrchestrationEventHandler {
  @Inject private BarrierService barrierService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    String planExecutionId = event.getAmbiance().getPlanExecutionId();
    try {
      Map<String, BarrierSetupInfo> barrierIdentifierSetupInfoMap =
          barrierService.getBarrierSetupInfoList(event.getAmbiance().getMetadata().getProcessedYaml())
              .stream()
              .collect(Collectors.toMap(BarrierSetupInfo::getIdentifier, Function.identity()));

      Map<String, List<BarrierPositionInfo.BarrierPosition>> barrierPositionInfoMap =
          barrierService.getBarrierPositionInfoList(event.getAmbiance().getMetadata().getProcessedYaml());

      List<BarrierExecutionInstance> barriers =
          barrierPositionInfoMap.entrySet()
              .stream()
              .filter(entry -> !entry.getValue().isEmpty())
              .map(entry
                  -> BarrierExecutionInstance.builder()
                         .uuid(generateUuid())
                         .setupInfo(barrierIdentifierSetupInfoMap.get(entry.getKey()))
                         .positionInfo(BarrierPositionInfo.builder()
                                           .planExecutionId(event.getAmbiance().getPlanExecutionId())
                                           .barrierPositionList(entry.getValue())
                                           .build())
                         .name(barrierIdentifierSetupInfoMap.get(entry.getKey()).getName())
                         .barrierState(Barrier.State.STANDING)
                         .identifier(entry.getKey())
                         .planExecutionId(event.getAmbiance().getPlanExecutionId())
                         .build())
              .collect(Collectors.toList());

      barrierService.saveAll(barriers);
    } catch (Exception e) {
      log.error("[{}] event failed for plan [{}]", event.getEventType(), planExecutionId);
      throw e;
    }
  }
}
