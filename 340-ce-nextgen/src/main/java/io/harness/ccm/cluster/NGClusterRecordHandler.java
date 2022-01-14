package io.harness.ccm.cluster;

import io.harness.ccm.commons.entities.ClusterRecord;
import io.harness.ccm.commons.service.intf.ClusterRecordService;

import com.google.inject.Inject;

public class NGClusterRecordHandler {
  private final ClusterRecordService clusterRecordService;

  @Inject
  private NGClusterRecordHandler(ClusterRecordService clusterRecordService) {
    this.clusterRecordService = clusterRecordService;
  }

  public ClusterRecord handleNewCEK8sConnectorCreate(ClusterRecord clusterRecord) {
    return clusterRecordService.upsert(clusterRecord);
  }

  public ClusterRecord getClusterRecord(String accountId, String ceK8sConnectorRef) {
    return clusterRecordService.getByCEK8sIdentifier(accountId, ceK8sConnectorRef);
  }

  public boolean deleteClusterRecord(String accountId, String ceK8sConnectorRef) {
    return clusterRecordService.delete(accountId, ceK8sConnectorRef);
  }

  public ClusterRecord attachPerpetualTask(ClusterRecord clusterRecord, String taskId) {
    return clusterRecordService.attachTask(clusterRecord, taskId);
  }

  public ClusterRecord getClusterRecordFromK8sBaseConnector(String accountId, String baseK8sConnectorRefIdentifier) {
    return clusterRecordService.get(accountId, baseK8sConnectorRefIdentifier);
  }
}
