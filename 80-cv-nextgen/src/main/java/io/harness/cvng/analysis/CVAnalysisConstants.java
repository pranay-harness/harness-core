package io.harness.cvng.analysis;

public interface CVAnalysisConstants {
  int MAX_RETRIES = 2;
  String LEARNING_RESOURCE = "learning";
  String TIMESERIES_ANALYSIS_RESOURCE = "timeseries-analysis";
  String LOG_CLUSTER_RESOURCE = "log-cluster";

  String LOG_ANALYSIS_RESOURCE = "log-analysis";
  String MARK_FAILURE_PATH = "mark-failure";
  String LOG_ANALYSIS_SAVE_PATH = "serviceguard-save-analysis";
  String PREVIOUS_LOG_ANALYSIS_PATH = "serviceguard-shortterm-history";
  String TEST_DATA_PATH = "test-data";
  int ML_RECORDS_TTL_MONTHS = 6;
}
