package io.harness.dto.converter;

import io.harness.ambiance.Ambiance;
import io.harness.dto.AmbianceDTO;
import lombok.experimental.UtilityClass;

import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class AmbianceDTOConverter {
  public Function<Ambiance, AmbianceDTO> toAmbianceDTO = ambiance
      -> AmbianceDTO.builder()
             .planExecutionId(ambiance.getPlanExecutionId())
             .setupAbstractions(ambiance.getSetupAbstractions())
             .levels(ambiance.getLevels().stream().map(LevelDTOConverter.toLevelDTO).collect(Collectors.toList()))
             .build();
}
