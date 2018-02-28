package software.wings.service.impl.sumo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.SumoConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@ToString(callSuper = true)
public class SumoDataCollectionInfo extends LogDataCollectionInfo {
  private SumoConfig sumoConfig;
  private String hostnameField;

  public SumoDataCollectionInfo(SumoConfig sumoConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, Set<String> queries, long startTime,
      int startMinute, int collectionTime, Set<String> hosts, List<EncryptedDataDetail> encryptedDataDetails,
      String hostnameField) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, queries, startTime,
        startMinute, collectionTime, hosts, StateType.SUMO, encryptedDataDetails);
    this.sumoConfig = sumoConfig;
    this.hostnameField = hostnameField;
  }
}