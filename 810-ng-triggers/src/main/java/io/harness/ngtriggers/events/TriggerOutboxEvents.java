/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.events;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CI)
public class TriggerOutboxEvents {
  public static final String TRIGGER_CREATED = "TriggerCreated";
  public static final String TRIGGER_UPDATED = "TriggerUpdated";
  public static final String TRIGGER_DELETED = "TriggerDeleted";
}
