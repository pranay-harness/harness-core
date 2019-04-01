package software.wings.service.impl.prometheus;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.PrometheusConfig;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
public class PrometheusDataCollectionInfo implements ExecutionCapabilityDemander {
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
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return prometheusConfig.fetchRequiredExecutionCapabilities();
  }
}
