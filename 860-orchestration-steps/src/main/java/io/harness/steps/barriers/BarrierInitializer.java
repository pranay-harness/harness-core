package io.harness.steps.barriers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.barrier.Barrier;
import io.harness.engine.observers.OrchestrationStartObserver;
import io.harness.pms.contracts.ambiance.Ambiance;
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
public class BarrierInitializer implements OrchestrationStartObserver {
  @Inject private BarrierService barrierService;

  @Override
  public void onStart(Ambiance ambiance) {
    String planExecutionId = ambiance.getPlanExecutionId();
    try {
      Map<String, BarrierSetupInfo> barrierIdentifierSetupInfoMap =
          barrierService.getBarrierSetupInfoList(ambiance.getMetadata().getProcessedYaml())
              .stream()
              .collect(Collectors.toMap(BarrierSetupInfo::getIdentifier, Function.identity()));

      Map<String, List<BarrierPositionInfo.BarrierPosition>> barrierPositionInfoMap =
          barrierService.getBarrierPositionInfoList(ambiance.getMetadata().getProcessedYaml());

      List<BarrierExecutionInstance> barriers =
          barrierPositionInfoMap.entrySet()
              .stream()
              .filter(entry -> !entry.getValue().isEmpty())
              .map(entry
                  -> BarrierExecutionInstance.builder()
                         .uuid(generateUuid())
                         .setupInfo(barrierIdentifierSetupInfoMap.get(entry.getKey()))
                         .positionInfo(BarrierPositionInfo.builder()
                                           .planExecutionId(ambiance.getPlanExecutionId())
                                           .barrierPositionList(entry.getValue())
                                           .build())
                         .name(barrierIdentifierSetupInfoMap.get(entry.getKey()).getName())
                         .barrierState(Barrier.State.STANDING)
                         .identifier(entry.getKey())
                         .planExecutionId(ambiance.getPlanExecutionId())
                         .build())
              .collect(Collectors.toList());

      barrierService.saveAll(barriers);
    } catch (Exception e) {
      log.error("Barrier initialization failed for planExecutionId: [{}]", planExecutionId);
      throw e;
    }
  }
}
