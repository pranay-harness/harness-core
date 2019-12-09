package software.wings.service.impl.analysis;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class MetricsDataCollectionInfo extends DataCollectionInfoV2 {
  private Map<String, String> hostsToGroupNameMap;
  public MetricsDataCollectionInfo(String accountId, String applicationId, String envId, Instant startTime,
      Instant endTime, Set<String> hosts, String cvConfigId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String cvTaskId, List<EncryptedDataDetail> encryptedDataDetails,
      Map<String, String> hostsToGroupNameMap) {
    super(accountId, applicationId, envId, startTime, endTime, hosts, cvConfigId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, cvTaskId, encryptedDataDetails);
    this.hostsToGroupNameMap = hostsToGroupNameMap;
  }

  public void setHostsToGroupNameMap(Map<String, String> hostsToGroupNameMap) {
    setHosts(hostsToGroupNameMap.keySet());
    this.hostsToGroupNameMap = hostsToGroupNameMap;
  }

  protected void copy(MetricsDataCollectionInfo metricsDataCollectionInfo) {
    super.copy(metricsDataCollectionInfo);
    metricsDataCollectionInfo.setHostsToGroupNameMap(new HashMap<>(hostsToGroupNameMap));
  }
}
