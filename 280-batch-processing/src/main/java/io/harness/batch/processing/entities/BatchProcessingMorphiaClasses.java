/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.entities;

import io.harness.ccm.commons.entities.batch.BatchJobScheduledData;
import io.harness.ccm.commons.entities.batch.InstanceData;
import io.harness.ccm.commons.entities.events.PublishedMessage;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class BatchProcessingMorphiaClasses {
  public static final Set<Class> classes =
      ImmutableSet.<Class>of(InstanceData.class, PublishedMessage.class, BatchJobScheduledData.class);

  private BatchProcessingMorphiaClasses() {}
}
