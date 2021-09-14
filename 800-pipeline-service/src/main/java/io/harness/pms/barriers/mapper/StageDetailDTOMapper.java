/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.barriers.mapper;

import io.harness.pms.barriers.response.StageDetailDTO;
import io.harness.steps.barriers.beans.StageDetail;

import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StageDetailDTOMapper {
  public final Function<StageDetail, StageDetailDTO> toStageDetailDTO =
      stageDetail -> StageDetailDTO.builder().name(stageDetail.getName()).build();
}
