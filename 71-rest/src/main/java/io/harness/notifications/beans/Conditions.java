package io.harness.notifications.beans;

import lombok.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Value
public class Conditions {
  @Nonnull private Operator operator;
  @Nullable private ManualInterventionAlertFilters manualInterventionFilters;
  @Nullable private CVAlertFilters cvAlertFilters;

  public enum Operator { MATCHING, NOT_MATCHING }
}
