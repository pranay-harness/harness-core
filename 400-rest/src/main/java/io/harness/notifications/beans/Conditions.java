/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notifications.beans;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Value;

@Value
public class Conditions {
  @Nonnull private Operator operator;
  @Nullable private ManualInterventionAlertFilters manualInterventionFilters;
  @Nullable private CVAlertFilters cvAlertFilters;

  public enum Operator { MATCHING, NOT_MATCHING }
}
