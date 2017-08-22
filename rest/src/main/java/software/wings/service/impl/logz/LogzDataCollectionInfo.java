package software.wings.service.impl.logz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.config.LogzConfig;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

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
  private String indices;
  private String hostnameField;
  private String messageField;

  public LogzDataCollectionInfo(LogzConfig logzConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, Set<String> queries, String indices,
      String hostnameField, String messageField, long startTime, int collectionTime, Set<String> hosts) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, queries, startTime,
        collectionTime, hosts, StateType.LOGZ);
    this.logzConfig = logzConfig;
    this.indices = indices;
    this.hostnameField = hostnameField;
    this.messageField = messageField;
  }
}
