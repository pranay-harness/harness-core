package software.wings.service.impl.logz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.config.LogzConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 8/21/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Builder
public class LogzDataCollectionInfo extends LogDataCollectionInfo {
  private LogzConfig logzConfig;
  private String hostnameField;
  private String messageField;
  private String timestampField;
  private String timestampFieldFormat;

  public LogzDataCollectionInfo(LogzConfig logzConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, Set<String> queries, String hostnameField,
      String messageField, String timestampField, String timestampFieldFormat, long startTime, int startMinute,
      int collectionTime, Set<String> hosts, List<EncryptedDataDetail> encryptedDataDetails) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, queries, startTime,
        startMinute, collectionTime, hosts, StateType.LOGZ, encryptedDataDetails);
    this.logzConfig = logzConfig;
    this.hostnameField = hostnameField;
    this.messageField = messageField;
    this.timestampField = timestampField;
    this.timestampFieldFormat = timestampFieldFormat;
  }
}
