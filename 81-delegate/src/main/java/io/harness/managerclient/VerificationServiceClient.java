package io.harness.managerclient;

import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;
import software.wings.common.VerificationConstants;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;
import software.wings.verification.CVActivityLog;

import java.util.List;

/**
 * Created by raghu on 09/17/18.
 */
public interface VerificationServiceClient {
  @POST(VerificationConstants.DELEGATE_DATA_COLLETION + "/save-metrics")
  Call<RestResponse<Boolean>> saveTimeSeriesMetrics(@Query("accountId") String accountId,
      @Query("applicationId") String applicationId, @Query("stateExecutionId") String stateExecutionId,
      @Query("delegateTaskId") String delegateTaskId, @Body List<NewRelicMetricDataRecord> metricData);

  @POST(VerificationConstants.DELEGATE_DATA_COLLETION + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  Call<RestResponse<Boolean>> saveLogs(@Query("accountId") String accountId, @Query("appId") String appId,
      @Query("cvConfigId") String cvConfigId, @Query("stateExecutionId") String stateExecutionId,
      @Query("workflowId") String workflowId, @Query("workflowExecutionId") String workflowExecutionId,
      @Query("serviceId") String serviceId, @Query("clusterLevel") ClusterLevel clusterLevel,
      @Query("delegateTaskId") String delegateTaskId, @Query("stateType") StateType stateType,
      @Body List<LogElement> metricData);
  @POST(VerificationConstants.DELEGATE_DATA_COLLETION + VerificationConstants.SAVE_CV_ACTIVITY_LOGS_PATH)
  Call<RestResponse<Void>> saveActivityLogs(
      @Query("accountId") String accountId, @Body List<CVActivityLog> activityLogs);

  @POST(VerificationConstants.DELEGATE_DATA_COLLETION + VerificationConstants.CV_TASK_STATUS_UPDATE_PATH)
  Call<RestResponse<Void>> updateCVTaskStatus(@Query("accountId") String accountId, @Query("cvTaskId") String cvTaskId,
      @Body DataCollectionTaskResult dataCollectionTaskResult);
}
