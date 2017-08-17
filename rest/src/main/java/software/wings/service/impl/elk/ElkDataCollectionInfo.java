package software.wings.service.impl.elk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.ElkConfig;
import software.wings.service.impl.analysis.LogDataCollectionInfo;

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

  public ElkDataCollectionInfo(ElkConfig elkConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, Set<String> queries, long startTime,
      int collectionTime, Set<String> hosts) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, queries, startTime,
        collectionTime, hosts);
    this.elkConfig = elkConfig;
  }
}
