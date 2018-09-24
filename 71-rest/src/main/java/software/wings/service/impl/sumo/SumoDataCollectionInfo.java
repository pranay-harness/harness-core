package software.wings.service.impl.sumo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.wings.beans.SumoConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class SumoDataCollectionInfo extends LogDataCollectionInfo {
  private SumoConfig sumoConfig;

  @Builder
  public SumoDataCollectionInfo(SumoConfig sumoConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, String query, long startTime, int startMinute,
      int collectionTime, String hostnameField, Set<String> hosts, List<EncryptedDataDetail> encryptedDataDetails) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, query, startTime,
        startMinute, collectionTime, hostnameField, hosts, StateType.SUMO, encryptedDataDetails);
    this.sumoConfig = sumoConfig;
  }
}