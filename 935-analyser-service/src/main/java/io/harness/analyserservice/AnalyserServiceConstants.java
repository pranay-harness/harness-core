/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
