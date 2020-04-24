package software.wings.service.impl.stackdriver;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.GcpConfig;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
public class StackDriverLogDataCollectionInfo extends LogDataCollectionInfo {
  private GcpConfig gcpConfig;
  private String logMessageField;

  @Builder
  public StackDriverLogDataCollectionInfo(GcpConfig gcpConfig, String accountId, String applicationId,
      String stateExecutionId, String cvConfigId, String workflowId, String workflowExecutionId, String serviceId,
      String query, long startTime, long endTime, int startMinute, int collectionTime, String hostnameField,
      Set<String> hosts, StateType stateType, List<EncryptedDataDetail> encryptedDataDetails, int initialDelayMinutes,
      String logMessageField) {
    super(accountId, applicationId, stateExecutionId, cvConfigId, workflowId, workflowExecutionId, serviceId, query,
        startTime, endTime, startMinute, collectionTime, hostnameField, hosts, stateType, encryptedDataDetails,
        initialDelayMinutes);
    this.gcpConfig = gcpConfig;
    this.logMessageField = logMessageField;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return StackdriverUtils.fetchRequiredExecutionCapabilitiesForLogs(getEncryptedDataDetails());
  }
}
