package io.harness.cvng.analysis;

public interface CVAnalysisConstants {
  int MAX_RETRIES = 2;
  String LEARNING_RESOURCE = "learning";
  String TIMESERIES_ANALYSIS_RESOURCE = "timeseries-analysis";

  String LOG_CLUSTER_RESOURCE = "log-cluster";

  String LOG_ANALYSIS_RESOURCE = "log-analysis";
  String MARK_FAILURE_PATH = "mark-failure";
  String LOG_ANALYSIS_SAVE_PATH = "serviceguard-save-analysis";
  String DEPLOYMENT_LOG_ANALYSIS_SAVE_PATH = "deployment-save-analysis";
  String PREVIOUS_LOG_ANALYSIS_PATH = "serviceguard-shortterm-history";
  String TEST_DATA_PATH = "test-data";
  int ML_RECORDS_TTL_MONTHS = 6;
  String TIMESERIES_SAVE_ANALYSIS_PATH = "/timeseries-serviceguard-save-analysis";
  String TIMESERIES_VERIFICATION_TASK_SAVE_ANALYSIS_PATH = "/timeseries-verification-task-save-analysis";
}
