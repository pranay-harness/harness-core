package software.wings.service.intfc.analysis;

/**
 * Created by rsingh on 8/7/17.
 */
public interface LogAnalysisResource {
  String LOG_ANALYSIS = "logml";

  String SPLUNK_RESOURCE_BASE_URL = "splunkv2";

  String ELK_RESOURCE_BASE_URL = "elk";

  String LOGZ_RESOURCE_BASE_URL = "logz";

  String SUMO_RESOURCE_BASE_URL = "sumo";

  /**
   * url for delegate to send collected logs
   */
  String ANALYSIS_STATE_SAVE_LOG_URL = "/save-logs";

  String ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL = "/save-24X7-clustered-logs";
  /**
   * url for python service to get collected logs
   */
  String ANALYSIS_STATE_GET_LOG_URL = "/get-logs";

  String ANALYSIS_GET_24X7_LOG_URL = "/get-24X7-logs";

  /**
   * url for python service to save analyzed records
   */
  String ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL = "/save-analysis-records";

  String ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL = "/save-24X7-analysis-records";

  /**
   * url for UI to get analysis summary
   */
  String ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL = "/get-analysis-records";

  String ANALYSIS_GET_24X7_ANALYSIS_RECORDS_URL = "/get-24X7-analysis-records";

  /**
   * url for python service to get analyzed records
   */
  String ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL = "/get-analysis-summary";

  /**
   * url for UI to get lits of indices
   */
  String ELK_GET_INDICES_URL = "/get-indices";

  /**
   * url for UI to get sample record
   */
  String ANALYSIS_STATE_GET_SAMPLE_RECORD_URL = "/get-sample-record";

  /**
   * url for UI to get sample record
   */
  String ANALYSIS_STATE_GET_HOST_RECORD_URL = "/get-host-records";

  /**
   * Ignore feedback from the user
   */
  String ANALYSIS_USER_FEEDBACK = "/user-feedback";

  /**
   * Url for UI to fetch User Feedbacks by workflow id
   */
  String ANALYSIS_USER_FEEDBACK_BY_WORKFLOW = "/user-feedback-by-workflow";

  /**
   * Validate query
   */
  String VALIDATE_QUERY = "/validate-query";

  String LAST_EXECUTION_NODES = "/last-execution-nodes";

  /**
   * url to Test configuration. fetches host and without host data
   */
  String TEST_NODE_DATA = "/node-data";
}
