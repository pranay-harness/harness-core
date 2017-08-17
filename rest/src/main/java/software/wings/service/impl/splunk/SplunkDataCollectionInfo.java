package software.wings.service.impl.splunk;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.analysis.LogDataCollectionInfo;

import java.util.Set;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
public class SplunkDataCollectionInfo extends LogDataCollectionInfo {
  private SplunkConfig splunkConfig;

  public SplunkDataCollectionInfo(SplunkConfig splunkConfig, String accountId, String applicationId,
      String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId, Set<String> queries,
      long startTime, int collectionTime, Set<String> hosts) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, queries, startTime,
        collectionTime, hosts);
    this.splunkConfig = splunkConfig;
  }
}
