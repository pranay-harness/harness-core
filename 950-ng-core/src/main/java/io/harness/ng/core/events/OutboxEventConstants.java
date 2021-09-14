/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class OutboxEventConstants {
  public static final String SERVICE_CREATED = "ServiceCreated";
  public static final String SERVICE_UPDATED = "ServiceUpdated";
  public static final String SERVICE_DELETED = "ServiceDeleted";
  public static final String SERVICE_UPSERTED = "ServiceUpserted";
  public static final String ENVIRONMENT_CREATED = "EnvironmentCreated";
  public static final String ENVIRONMENT_UPDATED = "EnvironmentUpdated";
  public static final String ENVIRONMENT_DELETED = "EnvironmentDeleted";
  public static final String ENVIRONMENT_UPSERTED = "EnvironmentUpserted";
}
