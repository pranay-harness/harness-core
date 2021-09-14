/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.analyserservice;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class AnalyserServiceConstants {
  public static final String SERVICE = "service";
  public static final String VERSION = "version";
  public static final String OLD_VERSION = "oldVersion";
  public static final String NEW_VERSION = "newVersion";
  public static final String ALERT_TYPE = "alertType";
  String SAMPLE_AGGREGATOR_SCHEDULED_THREAD = "analyserSampleAggregatorExecutor";
}
