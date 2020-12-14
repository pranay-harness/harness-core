package io.harness.pms.execution.utils;

import static io.harness.pms.contracts.advisers.AdviseType.INTERVENTION_WAIT;

import io.harness.pms.contracts.advisers.AdviseType;

import java.util.EnumSet;

public class AdviseTypeUtils {
  private static final EnumSet<AdviseType> WAITING_ADVISE_TYPES = EnumSet.of(INTERVENTION_WAIT);

  public static boolean isWaitingAdviseType(AdviseType adviseType) {
    return WAITING_ADVISE_TYPES.contains(adviseType);
  }
}
