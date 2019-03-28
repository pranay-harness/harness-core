package software.wings.service.impl.cloudwatch;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AwsConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
public class CloudWatchDataCollectionInfo implements ExecutionCapabilityDemander {
  private AwsConfig awsConfig;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvConfigId;
  private long startTime;
  private int collectionTime;
  private int dataCollectionMinute;
  private AnalysisComparisonStrategy analysisComparisonStrategy;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String region;

  @Builder.Default private Map<String, String> hosts = new HashMap<>();
  @Builder.Default private Map<String, List<CloudWatchMetric>> lambdaFunctionNames = new HashMap<>();
  @Builder.Default private Map<String, List<CloudWatchMetric>> loadBalancerMetrics = new HashMap<>();
  @Builder.Default private List<CloudWatchMetric> ec2Metrics = new ArrayList<>();
  @Builder.Default private Map<String, List<CloudWatchMetric>> metricsByECSClusterName = new HashMap<>();

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    // TODO: CAPABILITY
    return CapabilityHelper.generateDelegateCapabilities(awsConfig, encryptedDataDetails);
  }
}
