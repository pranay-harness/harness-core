package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.utils.Misc.replaceDotWithUnicode;
import static software.wings.utils.Misc.replaceUnicodeWithDot;

import com.google.common.base.Preconditions;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "MetricsDataCollectionInfoKeys")
public abstract class MetricsDataCollectionInfo extends DataCollectionInfoV2 {
  private Map<String, String> hostsToGroupNameMap;

  public MetricsDataCollectionInfo(String accountId, String applicationId, String envId, Instant startTime,
      Instant endTime, Set<String> hosts, String cvConfigId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String cvTaskId, String connectorId,
      List<EncryptedDataDetail> encryptedDataDetails, Instant dataCollectionStartTime,
      Map<String, String> hostsToGroupNameMap, boolean shouldSendHeartbeat) {
    super(accountId, applicationId, envId, startTime, endTime, hosts, cvConfigId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, cvTaskId, connectorId, encryptedDataDetails, dataCollectionStartTime,
        shouldSendHeartbeat);
    setHostsToGroupNameMap(hostsToGroupNameMap);
  }

  protected void copy(MetricsDataCollectionInfo metricsDataCollectionInfo) {
    super.copy(metricsDataCollectionInfo);
    metricsDataCollectionInfo.setHostsToGroupNameMap(new HashMap<>(hostsToGroupNameMap));
  }

  @Override
  public void validate() {
    super.validate();
    Preconditions.checkNotNull(hostsToGroupNameMap, "hostToGroupNameMap");
  }

  public final void setHostsToGroupNameMap(Map<String, String> hostsToGroupNameMap) {
    Map<String, String> updatedMap = new HashMap<>();
    if (isEmpty(hostsToGroupNameMap)) {
      this.hostsToGroupNameMap = updatedMap;
      return;
    }
    hostsToGroupNameMap.forEach((key, value) -> updatedMap.put(replaceDotWithUnicode(key), value));
    this.hostsToGroupNameMap = updatedMap;
  }

  public Map<String, String> getHostsToGroupNameMap() {
    Map<String, String> updatedMap = new HashMap<>();
    this.hostsToGroupNameMap.forEach((key, value) -> updatedMap.put(replaceUnicodeWithDot(key), value));
    return updatedMap;
  }
}
