package software.wings.service.impl.stackdriver;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.GcpConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.DataCollectionInfo;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;

import java.util.List;
import java.util.Map;

/**
 * Created by Pranjal on 11/27/2018
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StackDriverDataCollectionInfo extends DataCollectionInfo implements TaskParameters {
  private GcpConfig gcpConfig;
  private long startTime;
  private long endTime;

  private int startMinute;
  private int collectionTime;

  private int dataCollectionMinute;
  private int initialDelayMinutes;
  private TimeSeriesMlAnalysisType timeSeriesMlAnalysisType;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private Map<String, String> hosts;
  private Map<String, List<StackDriverMetric>> loadBalancerMetrics;
  private List<StackDriverMetric> podMetrics;

  @Builder
  public StackDriverDataCollectionInfo(String accountId, String applicationId, String stateExecutionId,
      String cvConfigId, String workflowId, String workflowExecutionId, String serviceId, GcpConfig gcpConfig,
      long startTime, long endTime, int startMinute, int collectionTime, int dataCollectionMinute,
      TimeSeriesMlAnalysisType timeSeriesMlAnalysisType, List<EncryptedDataDetail> encryptedDataDetails,
      Map<String, String> hosts, Map<String, List<StackDriverMetric>> loadBalancerMetrics,
      List<StackDriverMetric> podMetrics, int initialDelayMinutes) {
    super(accountId, applicationId, stateExecutionId, cvConfigId, workflowId, workflowExecutionId, serviceId);
    this.gcpConfig = gcpConfig;
    this.startTime = startTime;
    this.endTime = endTime;
    this.startMinute = startMinute;
    this.collectionTime = collectionTime;
    this.dataCollectionMinute = dataCollectionMinute;
    this.timeSeriesMlAnalysisType = timeSeriesMlAnalysisType;
    this.encryptedDataDetails = encryptedDataDetails;
    this.initialDelayMinutes = initialDelayMinutes;
    this.hosts = hosts;
    this.loadBalancerMetrics = loadBalancerMetrics;
    this.podMetrics = podMetrics;
  }
}
