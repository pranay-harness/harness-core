package software.wings.service.impl.apm;

import lombok.Builder;
import lombok.Data;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class APMDataCollectionInfo {
  private String baseUrl;
  private Map<String, String> headers;
  private Map<String, String> options;
  List<EncryptedDataDetail> encryptedDataDetails;
  Map<String, List<APMMetricInfo>> metricEndpoints;
  private Set<String> hosts;
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
}
