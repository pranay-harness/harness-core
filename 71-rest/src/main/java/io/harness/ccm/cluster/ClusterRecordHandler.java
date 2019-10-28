package io.harness.ccm.cluster;

import static java.util.Objects.isNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.ClusterRecord;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.InfrastructureMappingServiceObserver;
import software.wings.service.impl.SettingAttributeObserver;

@Slf4j
@Singleton
public class ClusterRecordHandler implements SettingAttributeObserver, InfrastructureMappingServiceObserver {
  private ClusterRecordService clusterRecordService;

  @Inject
  public ClusterRecordHandler(ClusterRecordService clusterRecordService) {
    this.clusterRecordService = clusterRecordService;
  }

  @Override
  public void onSaved(InfrastructureMapping infrastructureMapping) {
    upsertClusterRecord(infrastructureMapping);
  }

  @Override
  public void onUpdated(InfrastructureMapping infrastructureMapping) {
    upsertClusterRecord(infrastructureMapping);
  }

  private ClusterRecord upsertClusterRecord(InfrastructureMapping infrastructureMapping) {
    ClusterRecord clusterRecord = ClusterRecordUtils.from(infrastructureMapping);
    if (isNull(clusterRecord)) {
      logger.info("No Cluster can be derived from Infrastructure Mapping with id={}", infrastructureMapping.getUuid());
    } else {
      clusterRecordService.upsert(clusterRecord);
    }
    return clusterRecord;
  }

  @Override
  public void onDeleted(SettingAttribute settingAttribute) {
    deleteClusterRecord(settingAttribute);
  }

  private boolean deleteClusterRecord(SettingAttribute settingAttribute) {
    return clusterRecordService.delete(settingAttribute.getAccountId(), settingAttribute.getUuid());
  }
}
