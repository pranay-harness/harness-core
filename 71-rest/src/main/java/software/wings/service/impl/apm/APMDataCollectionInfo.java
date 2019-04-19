package software.wings.service.impl.apm;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class APMDataCollectionInfo implements TaskParameters {
  private String baseUrl;
  private String validationUrl;
  private Map<String, String> headers;
  private Map<String, String> options;
  List<EncryptedDataDetail> encryptedDataDetails;
  Map<String, List<APMMetricInfo>> metricEndpoints;
  private Map<String, String> hosts;
  private StateType stateType;
  private long startTime;
  private int dataCollectionMinute;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String accountId;
  private AnalysisComparisonStrategy strategy;
  private int dataCollectionFrequency;
  private int dataCollectionTotalTime;
  private String cvConfigId;
}
