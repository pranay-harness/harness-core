package software.wings.service.impl.analysis;

import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class LogDataCollectionInfoV2 extends DataCollectionInfoV2 implements ExecutionCapabilityDemander {
  private String query;
  private String hostnameField;
  private List<EncryptedDataDetail> encryptedDataDetails;

  public LogDataCollectionInfoV2(String accountId, String applicationId, String envId, Instant startTime,
      Instant endTime, Set<String> hosts, String cvConfigId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String cvTaskId, String query, String hostnameField,
      List<EncryptedDataDetail> encryptedDataDetails) {
    super(accountId, applicationId, envId, startTime, endTime, hosts, cvConfigId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, cvTaskId);
    this.query = query;
    this.hostnameField = hostnameField;
    this.encryptedDataDetails = encryptedDataDetails;
  }

  protected void copy(LogDataCollectionInfoV2 logDataCollectionInfo) {
    super.copy(logDataCollectionInfo);
    logDataCollectionInfo.setQuery(this.query);
    logDataCollectionInfo.setHostnameField(this.hostnameField);
    if (encryptedDataDetails != null) {
      logDataCollectionInfo.setEncryptedDataDetails(new ArrayList<>(encryptedDataDetails));
    }
  }
}
