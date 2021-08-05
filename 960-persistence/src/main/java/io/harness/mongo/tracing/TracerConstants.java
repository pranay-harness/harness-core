package io.harness.mongo.tracing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class TracerConstants {
  public static final String SERVICE_ID = "tracerServiceId";
  public static final String QUERY_HASH = "queryHash";
  // Append Service Name
  public static final String ANALYZER_CACHE_KEY = "queryAnalysisCache_%s";
  public static final String ANALYZER_CACHE_NAME = "queryAnalysisCache";
}
