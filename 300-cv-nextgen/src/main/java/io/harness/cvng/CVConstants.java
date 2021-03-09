package io.harness.cvng;

import java.time.Duration;

public interface CVConstants {
  String SERVICE_BASE_URL = "/cv/api";
  Duration VERIFICATION_JOB_INSTANCE_EXPIRY_DURATION = Duration.ofDays(30);
  /**
   * This should be set in findOption for the queries that are potentially working with large data.
   * This should be used for anything that is using
   * io.harness.cvng.utils.CVNGParallelExecutor#executeParallel(java.util.List) Ex: new
   * FindOptions().maxTime(MONGO_QUERY_TIMEOUT_SEC, TimeUnit.SECONDS);
   */
  int MONGO_QUERY_TIMEOUT_SEC = 5;
  double DEPLOYMENT_RISK_SCORE_FAILURE_THRESHOLD = 0.5;
  String DEFAULT_HEALTH_JOB_NAME = "Built-in health verification";
  String DEFAULT_HEALTH_JOB_ID = DEFAULT_HEALTH_JOB_NAME.replace(" ", "_");
  int STATE_MACHINE_IGNORE_LIMIT = 100;
  int STATE_MACHINE_IGNORE_MINUTES = 30;
}
