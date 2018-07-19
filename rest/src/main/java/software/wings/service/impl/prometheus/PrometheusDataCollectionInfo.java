package software.wings.service.impl.prometheus;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.PrometheusConfig;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.TimeSeries;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
public class PrometheusDataCollectionInfo {
  private PrometheusConfig prometheusConfig;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private long startTime;
  private int collectionTime;
  private List<TimeSeries> timeSeriesToCollect;
  private Map<String, String> hosts;
  private AnalysisComparisonStrategy analysisComparisonStrategy;
  private int dataCollectionMinute;
}
