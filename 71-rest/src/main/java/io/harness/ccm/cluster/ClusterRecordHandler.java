package io.harness.ccm.cluster;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class ClusterRecordHandler
    implements SettingAttributeObserver, InfrastructureDefinitionServiceObserver, InfrastructureMappingServiceObserver {
  private CCMSettingService ccmSettingService;
  private ClusterRecordService clusterRecordService;
  private CCMPerpetualTaskManager ccmPerpetualTaskManager;
  private InfrastructureDefinitionDao infrastructureDefinitionDao;
  private InfrastructureMappingDao infrastructureMappingDao;

  @Inject
  public ClusterRecordHandler(CCMSettingService ccmSettingService, ClusterRecordService clusterRecordService,
      CCMPerpetualTaskManager ccmPerpetualTaskManager, InfrastructureDefinitionDao infrastructureDefinitionDao,
      InfrastructureMappingDao infrastructureMappingDao) {
    this.ccmSettingService = ccmSettingService;
    this.clusterRecordService = clusterRecordService;
    this.ccmPerpetualTaskManager = ccmPerpetualTaskManager;
    this.infrastructureDefinitionDao = infrastructureDefinitionDao;
    this.infrastructureMappingDao = infrastructureMappingDao;
  }

  @Override
  public void onSaved(SettingAttribute cloudProvider) {
    upsertClusterRecord(cloudProvider);
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
    List<ClusterRecord> clusterRecords = new ArrayList<>();

    switch (cloudProvider.getValue().getType()) {
      case "AWS":
        List<InfrastructureDefinition> infrastructureDefinitions =
            infrastructureDefinitionDao.list(cloudProvider.getUuid());
        clusterRecords.addAll(Optional.ofNullable(infrastructureDefinitions)
                                  .map(Collection::stream)
                                  .orElseGet(Stream::empty)
                                  .map(id -> clusterRecordService.from(id))
                                  .collect(Collectors.toList()));
        List<InfrastructureMapping> infrastructureMappings = infrastructureMappingDao.list(cloudProvider.getUuid());
        clusterRecords.addAll(Optional.ofNullable(infrastructureMappings)
                                  .map(Collection::stream)
                                  .orElseGet(Stream::empty)
                                  .map(im -> clusterRecordService.from(im))
                                  .collect(Collectors.toList()));
        break;
      case "KUBERNETES_CLUSTER":
        clusterRecords = Arrays.asList(clusterRecordService.from(cloudProvider));
        break;
      default:
        break;
    }

    if (isNotEmpty(clusterRecords)) {
      for (ClusterRecord clusterRecord : clusterRecords) {
        clusterRecordService.upsert(clusterRecord);
      }
    }
  }

  @Override
  public void onDeleted(SettingAttribute settingAttribute) {
    deactivateClusterRecords(settingAttribute);
  }

  private ClusterRecord deactivateClusterRecords(SettingAttribute settingAttribute) {
    return clusterRecordService.deactivate(settingAttribute.getAccountId(), settingAttribute.getUuid());
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
