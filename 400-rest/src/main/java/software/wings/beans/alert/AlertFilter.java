/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.notifications.beans.Conditions;

import lombok.NonNull;
import lombok.Value;

@OwnedBy(PL)
@Value
public class AlertFilter {
  @NonNull private AlertType alertType;
  @NonNull private final Conditions conditions;
}
