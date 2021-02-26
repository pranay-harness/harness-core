package io.harness.pms.execution.utils;

import static io.harness.pms.contracts.advisers.AdviseType.END_PLAN;
import static io.harness.pms.contracts.advisers.AdviseType.INTERVENTION_WAIT;
import static io.harness.pms.contracts.advisers.AdviseType.NEXT_STEP;

import io.harness.pms.contracts.advisers.AdviseType;

import java.util.EnumSet;

public class AdviseTypeUtils {
  private static final EnumSet<AdviseType> WAITING_ADVISE_TYPES = EnumSet.of(INTERVENTION_WAIT);
  private static final EnumSet<AdviseType> TERMINAL_ADVISE_TYPES = EnumSet.of(NEXT_STEP, END_PLAN);

  public static boolean isWaitingAdviseType(AdviseType adviseType) {
    return WAITING_ADVISE_TYPES.contains(adviseType);
  }

  public static boolean isTerminalAdviseTypes(AdviseType adviseType) {
    return TERMINAL_ADVISE_TYPES.contains(adviseType);
  }
}
