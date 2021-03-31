package io.harness.cvng.analysis;

public interface CVAnalysisConstants {
  int MAX_RETRIES = 2;
  String LEARNING_RESOURCE = "learning";
  String TIMESERIES_ANALYSIS_RESOURCE = "timeseries-analysis";

  String LOG_CLUSTER_RESOURCE = "log-cluster";

  String LOG_ANALYSIS_RESOURCE = "log-analysis";
  String CD_10_MAPPING_RESOURCE = "cd10-mapping";
  String MARK_FAILURE_PATH = "mark-failure";
  String LOG_ANALYSIS_SAVE_PATH = "serviceguard-save-analysis";
  String DEPLOYMENT_LOG_ANALYSIS_SAVE_PATH = "deployment-save-analysis";
  String PREVIOUS_LOG_ANALYSIS_PATH = "serviceguard-shortterm-history";
  String PREVIOUS_ANALYSIS_URL = "previous-analysis";
  String TEST_DATA_PATH = "test-data";
  int ML_RECORDS_TTL_MONTHS = 6;
  String TIMESERIES_SAVE_ANALYSIS_PATH = "/timeseries-serviceguard-save-analysis";
  String TIMESERIES_VERIFICATION_TASK_SAVE_ANALYSIS_PATH = "/timeseries-verification-task-save-analysis";
  String CUMULATIVE_SUMS_URL = "timeseries-serviceguard-cumulative-sums";
  String PREVIOUS_ANOMALIES_URL = "timeseries-serviceguard-previous-anomalies";
  String SERVICE_GUARD_SHORT_TERM_HISTORY_URL = "timeseries-serviceguard-shortterm-history";

  String TREND_ANALYSIS_RESOURCE = "trend-analysis";
  String TREND_ANALYSIS_SAVE_PATH = "service-guard-trend-save-analysis";
  String TREND_ANALYSIS_TEST_DATA = "service-guard-trend-test-data";
  String TREND_METRIC_TEMPLATE = "trend-metric-template";
  String TREND_METRIC_NAME = "trend-error-metric";
  String LOG_METRIC_TEMPLATE_FILE = "/io/harness/cvng/analysis/service/impl/log_metric_template.yml";
  int ANALYSIS_RISK_RESULTS_LIMIT = 3;

  int TIMESERIES_SERVICE_GUARD_DATA_LENGTH = 135;
  int TIMESERIES_SERVICE_GUARD_WINDOW_SIZE = 15;
  int TREND_ANALYSIS_BASELINE_WINDOW_FOR_NEW_CLUSTER = 30;
  String LEARNING_ENGINE_TASK_METRIC = "le_task_queued_count";
}
