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

  public SumoDataCollectionInfo(String accountId, String applicationId, String stateExecutionId, String cvConfigId,
      String workflowId, String workflowExecutionId, String serviceId, String query, long startTime, long endTime,
      int startMinute, int collectionTime, String hostnameField, Set<String> hosts,
      List<EncryptedDataDetail> encryptedDataDetails, SumoConfig sumoConfig) {
    super(accountId, applicationId, stateExecutionId, cvConfigId, workflowId, workflowExecutionId, serviceId, query,
        startTime, endTime, startMinute, collectionTime, hostnameField, hosts, StateType.SUMO, encryptedDataDetails);
    this.sumoConfig = sumoConfig;
  }
}