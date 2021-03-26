package io.harness.pms.barriers.mapper;

import io.harness.pms.barriers.beans.BarrierExecutionInfo;
import io.harness.pms.barriers.response.BarrierInfoDTO;

import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BarrierInfoDTOMapper {
  public final Function<BarrierExecutionInfo, BarrierInfoDTO> toBarrierInfoDTO = barrierExecutionInfo
      -> BarrierInfoDTO.builder()
             .name(barrierExecutionInfo.getName())
             .timeoutIn(barrierExecutionInfo.getTimeoutIn())
             .stages(barrierExecutionInfo.getStages()
                         .stream()
                         .map(StageDetailDTOMapper.toStageDetailDTO)
                         .collect(Collectors.toList()))
             .build();
}
