package software.wings.service.intfc.analysis;

import software.wings.beans.RestResponse;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRequest;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogRequest;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 8/7/17.
 */
public interface LogAnalysisResource {
  String SPLUNK_RESOURCE_BASE_URL = "splunkv2";

  String ELK_RESOURCE_BASE_URL = "elk";

  String LOGZ_RESOURCE_BASE_URL = "logz";

  String SUMO_RESOURCE_BASE_URL = "sumo";

  /**
   * url for delegate to send collected logs
   */
  String ANALYSIS_STATE_SAVE_LOG_URL = "/save-logs";

  /**
   * url for python service to get collected logs
   */
  String ANALYSIS_STATE_GET_LOG_URL = "/get-logs";

  /**
   * url for python service to save analyzed records
   */
  String ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL = "/save-analysis-records";

  /**
   * url for UI to get analysis summary
   */
  String ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL = "/get-analysis-records";

  /**
   * url for python service to get analyzed records
   */
  String ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL = "/get-analysis-summary";

  /**
   * url for UI to get lits of indices
   */
  String ELK_GET_INDICES_URL = "/get-indices";

  /**
   * url for UI to get lits of indices
   */
  String KIBANA_GET_VERSION_URL = "/version";

  /**
   * url for UI to get sample record
   */
  String ANALYSIS_STATE_GET_SAMPLE_RECORD_URL = "/get-sample-record";

  RestResponse<Boolean> saveRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("workflowId") String workflowId,
      @QueryParam("workflowExecutionId") String workflowExecutionId, @QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @QueryParam("clusterLevel") ClusterLevel clusterLevel,
      @QueryParam("delegateTaskId") String delegateTaskId, List<LogElement> logData) throws IOException;

  RestResponse<List<LogDataRecord>> getRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("clusterLevel") ClusterLevel clusterLevel, @QueryParam("compareCurrent") boolean compareCurrent,
      LogRequest logRequest) throws IOException;

  RestResponse<Boolean> saveLogAnalysisMLRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("logCollectionMinute") Integer logCollectionMinute,
      @QueryParam("isBaselineCreated") boolean isBaselineCreated, @QueryParam("taskId") String taskId,
      LogMLAnalysisRecord mlAnalysisResponse) throws IOException;

  RestResponse<LogMLAnalysisRecord> getLogMLAnalysisRecords(
      @QueryParam("accountId") String accountId, LogMLAnalysisRequest mlAnalysisRequest) throws IOException;

  RestResponse<LogMLAnalysisSummary> getLogAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId)
      throws IOException;
}
