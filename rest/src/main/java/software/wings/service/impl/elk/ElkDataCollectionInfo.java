package software.wings.service.impl.elk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.ElkConfig;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.Set;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class ElkDataCollectionInfo extends LogDataCollectionInfo {
  private ElkConfig elkConfig;
  private String indices;
  private String hostnameField;
  private String messageField;

  public ElkDataCollectionInfo(ElkConfig elkConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, Set<String> queries, String indices,
      String hostnameField, String messageField, long startTime, int collectionTime, Set<String> hosts) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, queries, startTime,
        collectionTime, hosts, StateType.ELK);
    this.elkConfig = elkConfig;
    this.indices = indices;
    this.hostnameField = hostnameField;
    this.messageField = messageField;
  }
}
