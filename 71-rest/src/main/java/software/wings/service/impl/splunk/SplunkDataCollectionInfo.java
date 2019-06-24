package software.wings.service.impl.splunk;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.wings.beans.SplunkConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class SplunkDataCollectionInfo extends LogDataCollectionInfo implements ExecutionCapabilityDemander {
  private SplunkConfig splunkConfig;
  private boolean isAdvancedQuery;

  @Builder
  public SplunkDataCollectionInfo(String accountId, String applicationId, String stateExecutionId, String cvConfigId,
      String workflowId, String workflowExecutionId, String serviceId, String query, long startTime, long endTime,
      int startMinute, int collectionTime, String hostnameField, Set<String> hosts,
      List<EncryptedDataDetail> encryptedDataDetails, SplunkConfig splunkConfig, int initialDelayMinutes,
      boolean isAdvancedQuery) {
    super(accountId, applicationId, stateExecutionId, cvConfigId, workflowId, workflowExecutionId, serviceId, query,
        startTime, endTime, startMinute, collectionTime, hostnameField, hosts, StateType.SPLUNKV2, encryptedDataDetails,
        initialDelayMinutes);
    this.splunkConfig = splunkConfig;
    this.isAdvancedQuery = isAdvancedQuery;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(splunkConfig, getEncryptedDataDetails());
  }
}
