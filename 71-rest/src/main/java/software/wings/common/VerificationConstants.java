package software.wings.common;

import io.harness.event.model.EventConstants;
import software.wings.service.impl.appdynamics.AppdynamicsTimeSeries;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  public static final String STACKDRIVER_URL = "http://monitoring.googleapis.com";
  public static final String ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL = "/save-analysis-records";
  public static final String ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL = "/get-analysis-summary";
  public static final String ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL = "/get-exp-analysis-info";
  public static final String ANALYSIS_STATE_RE_QUEUE_TASK = "/experimentalTask";
  public static final String LEARNING_EXP_URL = "learning-exp";
  public static final String LOG_CLASSIFY_URL = "log-classify";
  public static final String GET_CLASSIFY_LABELS_URL = "/list-labels-to-classify";
  public static final String LAMBDA_HOST_NAME = "LAMBDA_HOST";
  public static final String DEFAULT_GROUP_NAME = "default";

  // DEMO Workflow Constants
  public static final String DEMO_APPLICAITON_ID = "CV-Demo";
  public static final String DEMO_WORKFLOW_EXECUTION_ID = "CV-Demo";
  public static final String DEMO_SUCCESS_LOG_STATE_EXECUTION_ID = "CV-Demo-LOG-Success-";
  public static final String DEMO_FAILURE_LOG_STATE_EXECUTION_ID = "CV-Demo-LOG-Failure-";
  public static final String DEMO_SUCCESS_TS_STATE_EXECUTION_ID = "CV-Demo-TS-Success-";
  public static final String DEMO_FAILURE_TS_STATE_EXECUTION_ID = "CV-Demo-TS-Failure-";

  public static final Double HIGH_RISK_CUTOFF = 0.5;
  public static final Double MEDIUM_RISK_CUTOFF = 0.3;
  public static final Double NO_DATA_CUTOFF = 0.0;
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
  public static final String IGNORED_ERRORS_METRIC_NAME = "ignored_errors";

  public static final String HEARTBEAT_METRIC_NAME = "Harness heartbeat metric";

  public static final String APPDYNAMICS_DEEPLINK_FORMAT =
      "#/location=METRIC_BROWSER&viewTree=true&axis=linear&showPoints=false&application={applicationId}"
      + "&timeRange=Custom_Time_Range.BETWEEN_TIMES.{endTimeMs}.{startTimeMs}.6&metrics=APPLICATION_COMPONENT.{metricString}";

  public static final String NEW_RELIC_DEEPLINK_FORMAT =
      "https://rpm.newrelic.com/set_time_window?back=https://rpm.newrelic.com/accounts/{accountId}/"
      + "applications/{applicationId}&tw[from_local]=true&tw[dur]={duration}&tw[end]={endTime}";

  public static final String PROMETHEUS_DEEPLINK_FORMAT =
      "{baseUrl}/graph?g0.range_input={rangeInput}m&g0.end_input={endTime}&"
      + "g0.expr={metricString}&g0.tab=0";

  public static final Map<String, String> ERROR_METRIC_NAMES =
      Collections.unmodifiableMap(new HashMap<String, String>() {
        {
          put(AppdynamicsTimeSeries.ERRORS_PER_MINUTE.getMetricName(), "Error Percentage");
          put(AppdynamicsTimeSeries.STALL_COUNT.getMetricName(), "Stall Count Percentage");
          put(NewRelicMetricValueDefinition.ERROR, "Error Percentage");
        }
      });

  private VerificationConstants() {}

  public static List<StateType> getMetricAnalysisStates() {
    return Arrays.asList(StateType.APM_VERIFICATION, StateType.APP_DYNAMICS, StateType.DATA_DOG, StateType.DYNA_TRACE,
        StateType.PROMETHEUS, StateType.NEW_RELIC, StateType.STACK_DRIVER, StateType.CLOUD_WATCH);
  }

  public static List<StateType> getLogAnalysisStates() {
    return Arrays.asList(
        StateType.ELK, StateType.SUMO, StateType.LOGZ, StateType.SPLUNK, StateType.SPLUNKV2, StateType.BUG_SNAG);
  }

  public static List<StateType> getAnalysisStates() {
    List<StateType> analysisStates = new ArrayList<>();
    analysisStates.addAll(getMetricAnalysisStates());
    analysisStates.addAll(getLogAnalysisStates());
    return analysisStates;
  }

  public static String getNewRelicMetricsYamlUrl() {
    return NEWRELIC_METRICS_YAML_URL;
  }

  public static final String[] DATA_COLLECTION_METRIC_LABELS = new String[] {
      EventConstants.ACCOUNT_ID, EventConstants.VERIFICATION_TYPE, EventConstants.VERIFICATION_247_CONFIGURED};

  public static final String[] IGNORED_ERRORS_METRIC_LABELS = new String[] {EventConstants.LOG_ML_FEEDBACKTYPE,
      EventConstants.VERIFICATION_STATE_TYPE, EventConstants.APPLICATION_ID, EventConstants.WORKFLOW_ID};

  public static String getDataCollectionMetricHelpDocument() {
    return "This metric is used to track the Verification data Collection per account";
  }

  public static String getDataAnalysisMetricHelpDocument() {
    return "This metric is used to track the Verification data analysis per account";
  }

  public static String getIgnoredErrorsMetricHelpDocument() {
    return "This metric is used to track the Ignored Errors";
  }
}
