package io.harness.dto.converter;

import io.harness.dto.AmbianceDTO;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AmbianceDTOConverter {
  public Function<Ambiance, AmbianceDTO> toAmbianceDTO = ambiance
      -> AmbianceDTO.builder()
             .planExecutionId(ambiance.getPlanExecutionId())
             .setupAbstractions(ambiance.getSetupAbstractionsMap())
             .levels(ambiance.getLevelsList().stream().map(LevelDTOConverter.toLevelDTO).collect(Collectors.toList()))
             .build();
}
