/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineOutboxEvents {
  public static final String INPUT_SET_CREATED = "InputSetCreated";
  public static final String INPUT_SET_UPDATED = "InputSetUpdated";
  public static final String INPUT_SET_DELETED = "InputSetDeleted";
  public static final String PIPELINE_CREATED = "PipelineCreated";
  public static final String PIPELINE_UPDATED = "PipelineUpdated";
  public static final String PIPELINE_DELETED = "PipelineDeleted";
}
