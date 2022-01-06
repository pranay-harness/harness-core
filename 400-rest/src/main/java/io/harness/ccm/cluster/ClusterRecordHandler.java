/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.cluster;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CEPerpetualTaskManager;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.config.CCMSettingService;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.CloudProviderObserver;
import software.wings.service.intfc.InfrastructureDefinitionServiceObserver;
import software.wings.service.intfc.InfrastructureMappingServiceObserver;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class ClusterRecordHandler
    implements CloudProviderObserver, InfrastructureDefinitionServiceObserver, InfrastructureMappingServiceObserver {
  private final CCMSettingService ccmSettingService;
  private final ClusterRecordService clusterRecordService;
  private final CEPerpetualTaskManager cePerpetualTaskManager;
  private final InfrastructureDefinitionDao infrastructureDefinitionDao;
  private final InfrastructureMappingDao infrastructureMappingDao;

  @Inject
  public ClusterRecordHandler(CCMSettingService ccmSettingService, ClusterRecordService clusterRecordService,
      CEPerpetualTaskManager cePerpetualTaskManager, InfrastructureDefinitionDao infrastructureDefinitionDao,
      InfrastructureMappingDao infrastructureMappingDao) {
    this.ccmSettingService = ccmSettingService;
    this.clusterRecordService = clusterRecordService;
    this.cePerpetualTaskManager = cePerpetualTaskManager;
    this.infrastructureDefinitionDao = infrastructureDefinitionDao;
    this.infrastructureMappingDao = infrastructureMappingDao;
  }

  @Override
  public void onSaved(SettingAttribute cloudProvider) {
    upsertClusterRecord(cloudProvider);
    if (cloudProvider.getValue().getType().equals(SettingVariableTypes.KUBERNETES_CLUSTER.name())
        && ccmSettingService.isCeK8sEventCollectionEnabled(cloudProvider)) {
      cePerpetualTaskManager.createPerpetualTasks(cloudProvider);
    }
  }

  @Override
  public void onUpdated(SettingAttribute prevCloudProvider, SettingAttribute currCloudProvider) {
    upsertClusterRecord(currCloudProvider);

    if (currCloudProvider.getValue().getType().equals(SettingVariableTypes.KUBERNETES_CLUSTER.name())) {
      // compare previous and current Cloud Providers
      boolean prevCeK8sEventCollectionEnabled = ccmSettingService.isCeK8sEventCollectionEnabled(prevCloudProvider);
      boolean currCeK8sEventCollectionEnabled = ccmSettingService.isCeK8sEventCollectionEnabled(currCloudProvider);
      // if the Cloud Provider has opted in k8s event collection
      if (!prevCeK8sEventCollectionEnabled && currCeK8sEventCollectionEnabled) {
        cePerpetualTaskManager.createPerpetualTasks(currCloudProvider);
      }
      // if only the Cloud Provider setting changed
      if (prevCeK8sEventCollectionEnabled && currCeK8sEventCollectionEnabled) {
        cePerpetualTaskManager.resetPerpetualTasks(currCloudProvider);
      }
      // if the Cloud provider has opted out of k8s event collection
      if (prevCeK8sEventCollectionEnabled && !currCeK8sEventCollectionEnabled) {
        cePerpetualTaskManager.deletePerpetualTasks(currCloudProvider);
      }

    } else {
      // compare previous and current Cloud Providers
      boolean prevCEEnabled = ccmSettingService.isCloudCostEnabled(prevCloudProvider);
      boolean currCEEnabled = ccmSettingService.isCloudCostEnabled(currCloudProvider);
      // if the Cloud Provider has ccm enabled
      if (!prevCEEnabled && currCEEnabled) {
        cePerpetualTaskManager.createPerpetualTasks(currCloudProvider);
      }
      // if only the Cloud Provider setting changed
      if (prevCEEnabled && currCEEnabled) {
        cePerpetualTaskManager.resetPerpetualTasks(currCloudProvider);
      }
      // if the Cloud provider has ccm disabled
      if (prevCEEnabled && !currCEEnabled) {
        cePerpetualTaskManager.deletePerpetualTasks(currCloudProvider);
      }
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
                                  .filter(cr -> cr != null)
                                  .collect(Collectors.toList()));
        List<InfrastructureMapping> infrastructureMappings = infrastructureMappingDao.list(cloudProvider.getUuid());
        clusterRecords.addAll(Optional.ofNullable(infrastructureMappings)
                                  .map(Collection::stream)
                                  .orElseGet(Stream::empty)
                                  .map(im -> clusterRecordService.from(im))
                                  .filter(cr -> cr != null)
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
    cePerpetualTaskManager.deletePerpetualTasks(settingAttribute);
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
      log.info("No Cluster can be derived from the Infrastructure Definition with id={}",
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
      log.info("No Cluster can be derived from Infrastructure Mapping with id={}", infrastructureMapping.getUuid());
    } else {
      clusterRecordService.upsert(clusterRecord);
    }
  }
}
