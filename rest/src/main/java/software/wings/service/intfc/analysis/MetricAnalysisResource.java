package software.wings.service.intfc.analysis;

import software.wings.beans.RestResponse;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.QueryParam;

/**
 * Created by sriram_parthasarathy on 12/4/17.
 */
public interface MetricAnalysisResource {
  RestResponse<Boolean> saveMetricData(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("delegateTaskId") String delegateTaskId, List<NewRelicMetricDataRecord> metricData)
      throws IOException;

  RestResponse<List<NewRelicMetricDataRecord>> getMetricData(@QueryParam("accountId") String accountId,
      @QueryParam("workflowExecutionId") String workFlowExecutionId,
      @QueryParam("compareCurrent") boolean compareCurrent, TSRequest request) throws IOException;

  RestResponse<NewRelicMetricAnalysisRecord> getMetricsAnalysis(@QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") String workflowExecutionId, @QueryParam("accountId") String accountId)
      throws IOException;

  RestResponse<Boolean> saveMLAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") String workflowExecutionId, @QueryParam("workflowId") String workflowId,
      @QueryParam("serviceId") String serviceId, @QueryParam("analysisMinute") Integer analysisMinute,
      @QueryParam("taskId") String taskId, TimeSeriesMLAnalysisRecord mlAnalysisResponse) throws IOException;

  RestResponse<List<NewRelicMetricHostAnalysisValue>> getTooltip(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workFlowExecutionId") String workFlowExecutionId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("transactionName") String transactionName,
      @QueryParam("metricName") String metricName) throws IOException;

  RestResponse<Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(@QueryParam("accountId") String accountId);
}
