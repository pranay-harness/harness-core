package software.wings.service.impl.splunk;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@ToString(callSuper = true)
public class SplunkDataCollectionInfo extends LogDataCollectionInfo {
  private SplunkConfig splunkConfig;

  @Builder
  public SplunkDataCollectionInfo(SplunkConfig splunkConfig, String accountId, String applicationId,
      String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId, String query,
      long startTime, int startMinute, int collectionTime, String hostnameField, Set<String> hosts,
      List<EncryptedDataDetail> encryptedDataDetails) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, query, startTime,
        startMinute, collectionTime, hostnameField, hosts, StateType.SPLUNKV2, encryptedDataDetails);
    this.splunkConfig = splunkConfig;
  }
}
