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
