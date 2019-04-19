package software.wings.service.impl.stackdriver;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.GcpConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Pranjal on 11/27/2018
 */
@Data
@Builder
public class StackDriverDataCollectionInfo implements TaskParameters {
  private GcpConfig gcpConfig;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvConfigId;
  private long startTime;
  private int collectionTime;
  private int dataCollectionMinute;
  private TimeSeriesMlAnalysisType timeSeriesMlAnalysisType;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Builder.Default private Map<String, String> hosts = new HashMap<>();
  @Builder.Default private Map<String, List<StackDriverMetric>> loadBalancerMetrics = new HashMap<>();
  @Builder.Default private Set<StackDriverMetric> vmInstanceMetrics = new HashSet<>();
}
