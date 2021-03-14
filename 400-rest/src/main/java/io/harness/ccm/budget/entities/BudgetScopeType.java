package io.harness.ccm.budget.entities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(Module._490_CE_COMMONS)
public class BudgetScopeType {
  public static final String APPLICATION = "APPLICATION";
  public static final String CLUSTER = "CLUSTER";
  public static final String PERSPECTIVE = "PERSPECTIVE";
}
