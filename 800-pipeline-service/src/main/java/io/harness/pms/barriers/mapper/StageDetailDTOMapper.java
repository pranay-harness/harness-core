package io.harness.pms.barriers.mapper;

import io.harness.pms.barriers.beans.StageDetail;
import io.harness.pms.barriers.response.StageDetailDTO;

import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StageDetailDTOMapper {
  public final Function<StageDetail, StageDetailDTO> toStageDetailDTO =
      stageDetail -> StageDetailDTO.builder().name(stageDetail.getName()).build();
}
