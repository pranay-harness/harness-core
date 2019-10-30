package io.harness.ccm.cluster;

import static java.util.Objects.isNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.CCMPerpetualTaskManager;
import io.harness.ccm.CCMSettingService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.InfrastructureMappingServiceObserver;
import software.wings.service.impl.SettingAttributeObserver;

@Slf4j
@Singleton
public class ClusterRecordHandler implements SettingAttributeObserver, InfrastructureMappingServiceObserver {
  private CCMSettingService ccmSettingService;
  private ClusterRecordService clusterRecordService;
  private CCMPerpetualTaskManager ccmPerpetualTaskManager;

  @Inject
  public ClusterRecordHandler(CCMSettingService ccmSettingService, ClusterRecordService clusterRecordService,
      CCMPerpetualTaskManager ccmPerpetualTaskManager) {
    this.ccmSettingService = ccmSettingService;
    this.clusterRecordService = clusterRecordService;
    this.ccmPerpetualTaskManager = ccmPerpetualTaskManager;
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
  public void onUpdated(SettingAttribute prevSettingAttribute, SettingAttribute currSettingAttribute) {
    // compare previous and current Cloud Providers
    boolean prevCCMEnabled = ccmSettingService.isCloudCostEnabled(prevSettingAttribute);
    boolean currCCMEnabled = ccmSettingService.isCloudCostEnabled(currSettingAttribute);
    // if the Cloud Provider has ccm enabled
    if (prevCCMEnabled == false && currCCMEnabled == true) {
      ccmPerpetualTaskManager.createPerpetualTasks(currSettingAttribute);
    }
    // if the Cloud Provider setting changed
    if (prevCCMEnabled == true && currCCMEnabled == true) {
      ccmPerpetualTaskManager.resetPerpetualTasks(currSettingAttribute);
    }
    // if the Cloud provider has ccm disabled
    if (prevCCMEnabled == true && currCCMEnabled == false) {
      ccmPerpetualTaskManager.deletePerpetualTasks(currSettingAttribute);
    }
  }

  @Override
  public void onDeleted(SettingAttribute settingAttribute) {
    deleteClusterRecords(settingAttribute);
  }

  private boolean deleteClusterRecords(SettingAttribute settingAttribute) {
    return clusterRecordService.delete(settingAttribute.getAccountId(), settingAttribute.getUuid());
  }
}
