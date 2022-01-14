package io.harness.ng.core.account;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public enum DefaultExperience {
  NG,
  CG;

  public static boolean isNGExperience(DefaultExperience defaultExperience) {
    return defaultExperience == DefaultExperience.NG;
  }

  public static boolean isCGExperience(DefaultExperience defaultExperience) {
    return defaultExperience == DefaultExperience.CG;
  }
}