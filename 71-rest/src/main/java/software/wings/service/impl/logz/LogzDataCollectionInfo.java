package software.wings.service.impl.logz;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.wings.beans.config.LogzConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 8/21/17.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class LogzDataCollectionInfo extends LogDataCollectionInfo implements ExecutionCapabilityDemander {
  private LogzConfig logzConfig;
  private String messageField;
  private String timestampField;
  private String timestampFieldFormat;
  private ElkQueryType queryType;

  @Builder
  public LogzDataCollectionInfo(String accountId, String applicationId, String stateExecutionId, String cvConfigId,
      String workflowId, String workflowExecutionId, String serviceId, String query, long startTime, long endTime,
      int startMinute, int collectionTime, String hostnameField, Set<String> hosts,
      List<EncryptedDataDetail> encryptedDataDetails, LogzConfig logzConfig, String messageField, String timestampField,
      String timestampFieldFormat, ElkQueryType queryType, int initialDelayMinutes) {
    super(accountId, applicationId, stateExecutionId, cvConfigId, workflowId, workflowExecutionId, serviceId, query,
        startTime, endTime, startMinute, collectionTime, hostnameField, hosts, StateType.LOGZ, encryptedDataDetails,
        initialDelayMinutes);
    this.logzConfig = logzConfig;
    this.messageField = messageField;
    this.timestampField = timestampField;
    this.timestampFieldFormat = timestampFieldFormat;
    this.queryType = queryType;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(logzConfig, getEncryptedDataDetails());
  }
}
