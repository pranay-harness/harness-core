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
  public static final String WORKFLOW_FOR_STATE_EXEC = "/workflow-execution-for-state-execution";
  public static final String CV_24x7_STATE_EXECUTION = "CV_24x7_EXECUTION";

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
