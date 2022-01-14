package io.harness.pms.barriers.mapper;

import io.harness.pms.barriers.beans.BarrierExecutionInfo;
import io.harness.pms.barriers.response.BarrierExecutionInfoDTO;

import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BarrierExecutionInfoDTOMapper {
  public final Function<BarrierExecutionInfo, BarrierExecutionInfoDTO> toBarrierExecutionInfoDTO = barrierExecutionInfo
      -> BarrierExecutionInfoDTO.builder()
             .name(barrierExecutionInfo.getName())
             .identifier(barrierExecutionInfo.getIdentifier())
             .startedAt(barrierExecutionInfo.getStartedAt())
             .started(barrierExecutionInfo.isStarted())
             .timeoutIn(barrierExecutionInfo.getTimeoutIn())
             .stages(barrierExecutionInfo.getStages()
                         .stream()
                         .map(StageDetailDTOMapper.toStageDetailDTO)
                         .collect(Collectors.toList()))
             .build();
}
