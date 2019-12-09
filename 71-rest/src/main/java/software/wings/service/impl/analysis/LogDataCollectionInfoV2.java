package software.wings.service.impl.analysis;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class LogDataCollectionInfoV2 extends DataCollectionInfoV2 {
  private String query;
  private String hostnameField;

  public LogDataCollectionInfoV2(String accountId, String applicationId, String envId, Instant startTime,
      Instant endTime, Set<String> hosts, String cvConfigId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String cvTaskId, List<EncryptedDataDetail> encryptedDataDetails,
      String query, String hostnameField) {
    super(accountId, applicationId, envId, startTime, endTime, hosts, cvConfigId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, cvTaskId, encryptedDataDetails);
    this.query = query;
    this.hostnameField = hostnameField;
  }

  protected void copy(LogDataCollectionInfoV2 logDataCollectionInfo) {
    super.copy(logDataCollectionInfo);
    logDataCollectionInfo.setQuery(this.query);
    logDataCollectionInfo.setHostnameField(this.hostnameField);
  }
}
