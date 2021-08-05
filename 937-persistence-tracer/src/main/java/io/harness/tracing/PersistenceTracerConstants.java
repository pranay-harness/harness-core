package io.harness.tracing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class PersistenceTracerConstants {
  public static final String TRACING_THREAD_POOL = "TracingThreadPool";
  public static final String QUERY_ANALYSIS_PRODUCER = "queryAnalysisProducer";
}
