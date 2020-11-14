package io.harness.dto.converter;

import io.harness.dto.LevelDTO;
import io.harness.pms.ambiance.Level;
import lombok.experimental.UtilityClass;

import java.util.function.Function;

@UtilityClass
public class LevelDTOConverter {
  public Function<Level, LevelDTO> toLevelDTO = level
      -> LevelDTO.builder()
             .identifier(level.getIdentifier())
             .setupId(level.getSetupId())
             .runtimeId(level.getRuntimeId())
             .group(level.getGroup())
             .stepType(level.getStepType().getType())
             .build();
}
