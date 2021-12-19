package io.harness.cvng;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;

@OwnedBy(HarnessTeam.CV)
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
  String DEFAULT_HEALTH_JOB_NAME = "Health Verification";
  String DEFAULT_HEALTH_JOB_ID = DEFAULT_HEALTH_JOB_NAME.replace(" ", "_");
  String DEFAULT_TEST_JOB_NAME = "Load Test Verification";
  String DEFAULT_TEST_JOB_ID = DEFAULT_TEST_JOB_NAME.replace(" ", "_");
  String DEFAULT_CANARY_JOB_NAME = "Canary Deployment Verification";
  String DEFAULT_CANARY_JOB_ID = DEFAULT_CANARY_JOB_NAME.replace(" ", "_");
  String DEFAULT_BLUE_GREEN_JOB_NAME = "Blue Green Deployment Verification";
  String DEFAULT_BLUE_GREEN_JOB_ID = DEFAULT_BLUE_GREEN_JOB_NAME.replace(" ", "_");
  String RUNTIME_PARAM_STRING = "<+input>";
  int STATE_MACHINE_IGNORE_LIMIT = 100;

  int CREATE_TIME_MINUTES_FOR_DEMO_CVCONFIG = 120;
  int DATA_COLLECTION_TIME_RANGE_FOR_SLI = 24 * 60;
  int STATE_MACHINE_IGNORE_MINUTES = 30;
  int STATE_MACHINE_IGNORE_MINUTES_FOR_DEMO = CREATE_TIME_MINUTES_FOR_DEMO_CVCONFIG + 120;
  int STATE_MACHINE_IGNORE_MINUTES_FOR_SLI = Integer.MAX_VALUE;

  String DATA_SOURCE_TYPE = "type";
  String SLO_TARGET_TYPE = "type";
  String SLI_METRIC_TYPE = "type";
  String LIVE_MONITORING = "live_monitoring";
  String DEPLOYMENT = "deployment";

  String TAG_DATA_SOURCE = "dataSource";
  String TAG_VERIFICATION_TYPE = "verificationType";
  String TAG_ACCOUNT_ID = "accountId";
  String TAG_ONBOARDING = "onboarding";
  String TAG_UNRECORDED = "unrecorded";
}
