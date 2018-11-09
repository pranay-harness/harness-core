package software.wings.common;

import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VerificationConstants {
  /*
  New Relic constants
   */
  private static final String NEWRELIC_METRICS_YAML_URL = "/apm/newrelic_metrics.yml";

  public static final long VERIFICATION_TASK_TIMEOUT = TimeUnit.MINUTES.toMillis(3);
  public static final String LAST_SUCCESSFUL_WORKFLOW_IDS = "/last-successful-workflow-ids";
  public static final String NOTIFY_METRIC_STATE = "/notify-metric-state";
  public static final String COLLECT_24_7_DATA = "/collect-24-7-data";
  public static final String NOTIFY_LOG_STATE = "/notify-log-state";
  public static final String CHECK_STATE_VALID = "/state-valid";
  public static final String GET_DEPLOYMENTS_24_7 = "/cv24-7-deployment-list";
  public static final String GET_DEPLOYMENTS_SERVICE_24_7 = "/service-deployment-list";
  public static final String GET_ALL_CV_EXECUTIONS = "/get-all-cv-executions";
  public static final String CV_DASH_GET_RECORDS = "/get-records";
  public static final String WORKFLOW_FOR_STATE_EXEC = "/workflow-execution-for-state-execution";
  public static final String CV_24x7_STATE_EXECUTION = "CV_24x7_EXECUTION";
  public static final String HEATMAP = "/heatmap";
  public static final String VERIFICATION_SERVICE_BASE_URL = "/verification";
  public static final String HEATMAP_SUMMARY = "/heatmap-summary";
  public static final String TIMESERIES = "/timeseries";

  public static final int DURATION_TO_ASK_MINUTES = 5;
  public static final int CANARY_DAYS_TO_COLLECT = 7;
  public static final int PERIODIC_GAP_IN_DAYS = 7;
  public static final int TIME_DELAY_QUERY_MINS = 2;

  public static final int CRON_POLL_INTERVAL_IN_MINUTES = 15;
  public static final long CRON_POLL_INTERVAL = TimeUnit.MINUTES.toSeconds(CRON_POLL_INTERVAL_IN_MINUTES); // in seconds

  public static final int MIN_TIMESERIES_QUERY_INTERVAL_IN_HOURS = 1;
  public static final int MAX_TIMESERIES_QUERY_INTERVAL_IN_HOURS = 12;

  public static final String DATA_COLLECTION_TASKS_PER_MINUTE = "data_collection_tasks_per_min";
  public static final String DATA_ANALYSIS_TASKS_PER_MINUTE = "data_analysis_tasks_per_min";

  private VerificationConstants() {}

  public static List<StateType> getMetricAnalysisStates() {
    return Arrays.asList(StateType.APM_VERIFICATION, StateType.APP_DYNAMICS, StateType.DATA_DOG, StateType.DYNA_TRACE,
        StateType.PROMETHEUS, StateType.NEW_RELIC);
  }

  public static List<StateType> getLogAnalysisStates() {
    return Arrays.asList(StateType.ELK, StateType.SUMO, StateType.LOGZ, StateType.SPLUNK, StateType.SPLUNKV2,
        StateType.CLOUD_WATCH, StateType.BUG_SNAG);
  }

  public static String getNewRelicMetricsYamlUrl() {
    return NEWRELIC_METRICS_YAML_URL;
  }
}
