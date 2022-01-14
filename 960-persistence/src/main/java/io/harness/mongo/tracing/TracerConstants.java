package io.harness.mongo.tracing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class TracerConstants {
  public static final String SERVICE_ID = "tracerServiceId";
  public static final String QUERY_HASH = "queryHash";
}
