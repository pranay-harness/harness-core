package io.harness.pms.barriers.mapper;

import io.harness.pms.barriers.response.BarrierSetupInfoDTO;
import io.harness.steps.barriers.beans.BarrierSetupInfo;

import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BarrierSetupInfoDTOMapper {
  public final Function<BarrierSetupInfo, BarrierSetupInfoDTO> toBarrierSetupInfoDTO = barrierSetupInfo
      -> BarrierSetupInfoDTO.builder()
             .name(barrierSetupInfo.getName())
             .identifier(barrierSetupInfo.getIdentifier())
             .stages(barrierSetupInfo.getStages()
                         .stream()
                         .map(StageDetailDTOMapper.toStageDetailDTO)
                         .collect(Collectors.toList()))
             .build();
}
