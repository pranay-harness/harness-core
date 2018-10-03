package software.wings.verification;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;
import software.wings.beans.RestResponse;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.util.List;

/**
 * Created by raghu on 09/17/18.
 */
public interface VerificationServiceClient {
  @POST(MetricDataAnalysisService.RESOURCE_URL + "/save-metrics")
  Call<RestResponse<Boolean>> saveTimeSeriesMetrics(@Query("accountId") String accountId,
      @Query("applicationId") String applicationId, @Query("stateExecutionId") String stateExecutionId,
      @Query("delegateTaskId") String delegateTaskId, @Body List<NewRelicMetricDataRecord> metricData);

  @POST(LogAnalysisResource.LOG_ANALYSIS + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  Call<RestResponse<Boolean>> saveLogs(@Query("accountId") String accountId, @Query("appId") String appId,
      @Query("stateExecutionId") String stateExecutionId, @Query("workflowId") String workflowId,
      @Query("workflowExecutionId") String workflowExecutionId, @Query("serviceId") String serviceId,
      @Query("clusterLevel") ClusterLevel clusterLevel, @Query("delegateTaskId") String delegateTaskId,
      @Query("stateType") StateType stateType, @Body List<LogElement> metricData);
}
