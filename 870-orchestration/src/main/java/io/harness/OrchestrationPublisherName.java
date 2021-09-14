/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

// TODO : Rename this to OrchestrationConfigConstants
@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationPublisherName {
  public static final String PUBLISHER_NAME = "orchestrationPublisherName";

  public static final String PERSISTENCE_LAYER = "wePersistenceLayer";
}
