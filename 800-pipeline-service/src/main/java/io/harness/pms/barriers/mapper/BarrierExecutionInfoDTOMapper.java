/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
