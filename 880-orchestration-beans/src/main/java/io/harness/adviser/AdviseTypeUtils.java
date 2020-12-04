package io.harness.adviser;

import static io.harness.pms.advisers.AdviseType.INTERVENTION_WAIT;

import java.util.EnumSet;

public class AdviseTypeUtils {
  private static final EnumSet<io.harness.pms.advisers.AdviseType> WAITING_ADVISE_TYPES = EnumSet.of(INTERVENTION_WAIT);

  public static boolean isWaitingAdviseType(io.harness.pms.advisers.AdviseType adviseType) {
    return WAITING_ADVISE_TYPES.contains(adviseType);
  }
}
