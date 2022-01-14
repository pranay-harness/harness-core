package io.harness.dto.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.LevelDTO;
import io.harness.pms.contracts.ambiance.Level;

import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class LevelDTOConverter {
  public Function<Level, LevelDTO> toLevelDTO = level
      -> LevelDTO.builder()
             .identifier(level.getIdentifier())
             .setupId(level.getSetupId())
             .runtimeId(level.getRuntimeId())
             .group(level.getGroup())
             .stepType(level.getStepType().getType())
             .skipExpressionChain(level.getSkipExpressionChain())
             .build();
}
