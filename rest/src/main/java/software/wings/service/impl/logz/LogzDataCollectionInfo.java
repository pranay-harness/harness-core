package software.wings.service.impl.logz;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.wings.beans.config.LogzConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 8/21/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class LogzDataCollectionInfo extends LogDataCollectionInfo {
  private LogzConfig logzConfig;
  private String messageField;
  private String timestampField;
  private String timestampFieldFormat;
  private ElkQueryType queryType;

  @Builder
  public LogzDataCollectionInfo(LogzConfig logzConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, String query, String messageField,
      String timestampField, String timestampFieldFormat, ElkQueryType queryType, long startTime, int startMinute,
      int collectionTime, String hostnameField, Set<String> hosts, List<EncryptedDataDetail> encryptedDataDetails) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, query, startTime,
        startMinute, collectionTime, hostnameField, hosts, StateType.LOGZ, encryptedDataDetails);
    this.logzConfig = logzConfig;
    this.messageField = messageField;
    this.timestampField = timestampField;
    this.timestampFieldFormat = timestampFieldFormat;
    this.queryType = queryType;
  }
}
