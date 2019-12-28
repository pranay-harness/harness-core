package io.harness.ccm.cluster;

import static software.wings.settings.SettingValue.SettingVariableTypes.KUBERNETES_CLUSTER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.CCMPerpetualTaskManager;
import io.harness.ccm.CCMSettingService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.SettingAttributeObserver;
import software.wings.service.intfc.InfrastructureDefinitionServiceObserver;
import software.wings.service.intfc.InfrastructureMappingServiceObserver;

@Slf4j
@Singleton
public class ClusterRecordHandler
    implements SettingAttributeObserver, InfrastructureDefinitionServiceObserver, InfrastructureMappingServiceObserver {
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
  public void onSaved(SettingAttribute cloudProvider) {
    if (KUBERNETES_CLUSTER.name().equals(cloudProvider.getValue().getType())) {
      upsertClusterRecord(cloudProvider);
    }
  }

  @Override
  public void onUpdated(SettingAttribute prevCloudProvider, SettingAttribute currCloudProvider) {
    upsertClusterRecord(currCloudProvider);
    // compare previous and current Cloud Providers
    boolean prevCCMEnabled = ccmSettingService.isCloudCostEnabled(prevCloudProvider);
    boolean currCCMEnabled = ccmSettingService.isCloudCostEnabled(currCloudProvider);
    // if the Cloud Provider has ccm enabled
    if (!prevCCMEnabled && currCCMEnabled) {
      ccmPerpetualTaskManager.createPerpetualTasks(currCloudProvider);
    }
    // if the Cloud Provider setting changed
    if (prevCCMEnabled && currCCMEnabled) {
      ccmPerpetualTaskManager.resetPerpetualTasks(currCloudProvider);
    }
    // if the Cloud provider has ccm disabled
    if (prevCCMEnabled && !currCCMEnabled) {
      ccmPerpetualTaskManager.deletePerpetualTasks(currCloudProvider);
    }
  }

  private void upsertClusterRecord(SettingAttribute cloudProvider) {
    ClusterRecord clusterRecord = clusterRecordService.from(cloudProvider);
    clusterRecordService.upsert(clusterRecord);
  }

  @Override
  public void onDeleted(SettingAttribute settingAttribute) {
    deleteClusterRecords(settingAttribute);
  }

  private boolean deleteClusterRecords(SettingAttribute settingAttribute) {
    return clusterRecordService.delete(settingAttribute.getAccountId(), settingAttribute.getUuid());
  }

  @Override
  public void onSaved(InfrastructureDefinition infrastructureDefinition) {
    upsertClusterRecord(infrastructureDefinition);
  }

  @Override
  public void onUpdated(InfrastructureDefinition infrastructureDefinition) {
    upsertClusterRecord(infrastructureDefinition);
  }

  private void upsertClusterRecord(InfrastructureDefinition infrastructureDefinition) {
    ClusterRecord clusterRecord = clusterRecordService.from(infrastructureDefinition);
    if (clusterRecord == null) {
      logger.info("No Cluster can be derived from the Infrastructure Definition with id={}",
          infrastructureDefinition.getUuid());
    } else {
      clusterRecordService.upsert(clusterRecord);
    }
  }

  @Override
  public void onSaved(InfrastructureMapping infrastructureMapping) {
    upsertClusterRecord(infrastructureMapping);
  }

  @Override
  public void onUpdated(InfrastructureMapping infrastructureMapping) {
    upsertClusterRecord(infrastructureMapping);
  }

  private void upsertClusterRecord(InfrastructureMapping infrastructureMapping) {
    ClusterRecord clusterRecord = clusterRecordService.from(infrastructureMapping);
    if (clusterRecord == null) {
      logger.info("No Cluster can be derived from Infrastructure Mapping with id={}", infrastructureMapping.getUuid());
    } else {
      clusterRecordService.upsert(clusterRecord);
    }
  }
}
