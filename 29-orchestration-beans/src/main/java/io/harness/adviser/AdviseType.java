package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.EnumSet;

@OwnedBy(CDC)
public enum AdviseType {
  NEXT_STEP,
  RETRY,
  END_PLAN,
  INTERVENTION_WAIT;

  private static final EnumSet<AdviseType> WAITING_ADVISE_TYPES = EnumSet.of(INTERVENTION_WAIT);

  public static boolean isWaitingAdviseType(AdviseType adviseType) {
    return WAITING_ADVISE_TYPES.contains(adviseType);
  }
}
