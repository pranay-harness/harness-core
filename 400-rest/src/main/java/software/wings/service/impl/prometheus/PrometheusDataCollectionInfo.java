package software.wings.service.impl.prometheus;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import software.wings.beans.PrometheusConfig;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
public class PrometheusDataCollectionInfo implements TaskParameters, ExecutionCapabilityDemander {
  private PrometheusConfig prometheusConfig;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvConfigId;
  private long startTime;
  private int collectionTime;
  private List<TimeSeries> timeSeriesToCollect;
  private Map<String, String> hosts;
  private TimeSeriesMlAnalysisType timeSeriesMlAnalysisType;
  private int dataCollectionMinute;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return prometheusConfig.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}
