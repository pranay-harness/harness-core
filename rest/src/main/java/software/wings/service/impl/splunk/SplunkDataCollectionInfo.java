package software.wings.service.impl.splunk;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.SplunkConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@ToString(callSuper = true)
public class SplunkDataCollectionInfo extends LogDataCollectionInfo {
  private SplunkConfig splunkConfig;

  public SplunkDataCollectionInfo(SplunkConfig splunkConfig, String accountId, String applicationId,
      String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId, Set<String> queries,
      long startTime, int startMinute, int collectionTime, Set<String> hosts,
      List<EncryptedDataDetail> encryptedDataDetails) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, queries, startTime,
        startMinute, collectionTime, hosts, StateType.SPLUNKV2, encryptedDataDetails);
    this.splunkConfig = splunkConfig;
  }
}
