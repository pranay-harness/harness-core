package software.wings.service.impl.sumo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.Set;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
public class SumoDataCollectionInfo extends LogDataCollectionInfo {
  private SumoConfig sumoConfig;

  public SumoDataCollectionInfo(SumoConfig sumoConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, Set<String> queries, long startTime,
      int startMinute, int collectionTime, Set<String> hosts) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, queries, startTime,
        startMinute, collectionTime, hosts, StateType.SUMO);
    this.sumoConfig = sumoConfig;
  }
}