package software.wings.service.impl.apm;

import lombok.Builder;
import lombok.Data;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class APMDataCollectionInfo {
  private String baseUrl;
  List<List<APMMetricInfo>> metricEndpoints;
  List<EncryptedDataDetail> encryptedDataDetails;
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
}
