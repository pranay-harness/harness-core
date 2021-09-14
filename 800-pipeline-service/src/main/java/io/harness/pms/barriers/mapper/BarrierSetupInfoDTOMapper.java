/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
