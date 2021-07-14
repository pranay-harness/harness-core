package io.harness.service.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.DeploymentSummaryDTO;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.instancesyncperpetualtaskinfo.DeploymentInfoDetailsDTO;
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.models.DeploymentEvent;
import io.harness.models.constants.InstanceSyncConstants;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;
import io.harness.service.instancesynchandlerfactory.InstanceSyncHandlerFactoryService;
import io.harness.service.instancesyncperpetualtask.InstanceSyncPerpetualTaskService;
import io.harness.service.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceSyncServiceImpl implements InstanceSyncService {
  private PersistentLocker persistentLocker;
  private InstanceSyncPerpetualTaskService instanceSyncPerpetualTaskService;
  private InstanceSyncPerpetualTaskInfoService instanceSyncPerpetualTaskInfoService;
  private InstanceSyncHandlerFactoryService instanceSyncHandlerFactoryService;

  private static final int NEW_DEPLOYMENT_EVENT_RETRY = 3;

  @Override
  public void processInstanceSyncForNewDeployment(DeploymentEvent deploymentEvent) {
    int retryCount = 0;
    while (retryCount < NEW_DEPLOYMENT_EVENT_RETRY) {
      DeploymentSummaryDTO deploymentSummaryDTO = deploymentEvent.getDeploymentSummaryDTO();
      try (AcquiredLock acquiredLock = persistentLocker.waitToAcquireLock(
               InstanceSyncConstants.INSTANCE_SYNC_PREFIX + deploymentSummaryDTO.getInfrastructureMappingId(),
               InstanceSyncConstants.INSTANCE_SYNC_LOCK_TIMEOUT, InstanceSyncConstants.INSTANCE_SYNC_WAIT_TIMEOUT)) {
        InfrastructureMappingDTO infrastructureMappingDTO = deploymentSummaryDTO.getInfrastructureMapping();
        AbstractInstanceSyncHandler abstractInstanceSyncHandler =
            instanceSyncHandlerFactoryService.getInstanceSyncHandler(
                infrastructureMappingDTO.getInfrastructureMappingType());

        // check if existing instance sync perpetual task info record exists or not for incoming infrastructure mapping
        Optional<InstanceSyncPerpetualTaskInfoDTO> instanceSyncPerpetualTaskInfoDTOOptional =
            instanceSyncPerpetualTaskInfoService.findByInfrastructureMappingId(
                infrastructureMappingDTO.getAccountIdentifier(), infrastructureMappingDTO.getOrgIdentifier(),
                infrastructureMappingDTO.getProjectIdentifier(), infrastructureMappingDTO.getId());
        if (!instanceSyncPerpetualTaskInfoDTOOptional.isPresent()) {
          // no existing perpetual task info record found for given infrastructure mapping id
          // so create a new perpetual task and instance sync perpetual task info record
          String perpetualTaskId = instanceSyncPerpetualTaskService.createPerpetualTask(
              infrastructureMappingDTO, abstractInstanceSyncHandler);
          instanceSyncPerpetualTaskInfoService.save(
              prepareInstanceSyncPerpetualTaskInfoDTO(deploymentSummaryDTO, perpetualTaskId));
        } else {
          InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO =
              instanceSyncPerpetualTaskInfoDTOOptional.get();
          if (!abstractInstanceSyncHandler.isDeploymentInfoMatching(deploymentSummaryDTO.getDeploymentInfoDTO(),
                  getDeploymentInfoDTOList(instanceSyncPerpetualTaskInfoDTO.getDeploymentInfoDetailsDTOList()))) {
            // it means deployment info doesn't exist in the perpetual task info, we need to add it

            // reset the perpetual task so that it triggers its next execution right away
            instanceSyncPerpetualTaskService.resetPerpetualTask(instanceSyncPerpetualTaskInfoDTO.getAccountIdentifier(),
                instanceSyncPerpetualTaskInfoDTO.getPerpetualTaskId());

            // add the deploymentinfo and deployment summary id to the instance sync pt info record
            addNewDeploymentInfoToInstanceSyncPerpetualTaskInfoRecord(
                instanceSyncPerpetualTaskInfoDTO, deploymentSummaryDTO.getDeploymentInfoDTO());
            addNewDeploymentSummaryIdToInstanceSyncPerpetualTaskInfoRecord(
                instanceSyncPerpetualTaskInfoDTO, deploymentSummaryDTO.getId());
            instanceSyncPerpetualTaskInfoService.save(instanceSyncPerpetualTaskInfoDTO);
          }
        }

        // TODO add a success log here
        return;
      } catch (Exception exception) {
        // TODO log the exception here with proper error message
        retryCount += 1;
      }
    }

    // TODO add log here to mark the event as failed even after all retries
  }

  // ------------------------------- PRIVATE METHODS --------------------------------------

  private InstanceSyncPerpetualTaskInfoDTO prepareInstanceSyncPerpetualTaskInfoDTO(
      DeploymentSummaryDTO deploymentSummaryDTO, String perpetualTaskId) {
    InfrastructureMappingDTO infrastructureMappingDTO = deploymentSummaryDTO.getInfrastructureMapping();
    return InstanceSyncPerpetualTaskInfoDTO.builder()
        .accountIdentifier(infrastructureMappingDTO.getAccountIdentifier())
        .projectIdentifier(infrastructureMappingDTO.getProjectIdentifier())
        .orgIdentifier(infrastructureMappingDTO.getOrgIdentifier())
        .infrastructureMappingId(infrastructureMappingDTO.getId())
        .deploymentSummaryIdList(Collections.singletonList(deploymentSummaryDTO.getId()))
        .deploymentInfoDetailsDTOList(
            Collections.singletonList(DeploymentInfoDetailsDTO.builder()
                                          .deploymentInfoDTO(deploymentSummaryDTO.getDeploymentInfoDTO())
                                          .lastUsedAt(System.currentTimeMillis())
                                          .build()))
        .perpetualTaskId(perpetualTaskId)
        .build();
  }

  private List<DeploymentInfoDTO> getDeploymentInfoDTOList(
      List<DeploymentInfoDetailsDTO> deploymentInfoDetailsDTOList) {
    List<DeploymentInfoDTO> deploymentInfoDTOList = new ArrayList<>();
    deploymentInfoDetailsDTOList.forEach(
        deploymentInfoDetailsDTO -> deploymentInfoDTOList.add(deploymentInfoDetailsDTO.getDeploymentInfoDTO()));
    return deploymentInfoDTOList;
  }

  private void addNewDeploymentInfoToInstanceSyncPerpetualTaskInfoRecord(
      InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO, DeploymentInfoDTO deploymentInfoDTO) {
    instanceSyncPerpetualTaskInfoDTO.getDeploymentInfoDetailsDTOList().add(DeploymentInfoDetailsDTO.builder()
                                                                               .deploymentInfoDTO(deploymentInfoDTO)
                                                                               .lastUsedAt(System.currentTimeMillis())
                                                                               .build());
  }

  private void addNewDeploymentSummaryIdToInstanceSyncPerpetualTaskInfoRecord(
      InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO, String deploymentSummmaryId) {
    instanceSyncPerpetualTaskInfoDTO.getDeploymentSummaryIdList().add(deploymentSummmaryId);
  }
}
