package io.harness.service.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.DeploymentSummaryDTO;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.dtos.InstanceDTO;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.dtos.instancesyncperpetualtaskinfo.DeploymentInfoDetailsDTO;
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.models.DeploymentEvent;
import io.harness.models.constants.InstanceSyncConstants;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.service.instance.InstanceService;
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
  private InstanceService instanceService;
  private EnvironmentService environmentService;
  private ServiceEntityService serviceEntityService;

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

        // Create fresh instances for the new deployment
        List<InstanceInfoDTO> instanceInfoDTOList = abstractInstanceSyncHandler.getInstanceDetailsFromServerInstances(
            deploymentSummaryDTO.getServerInstanceInfoList());
        instanceInfoDTOList.forEach(instanceInfoDTO
            -> buildAndSaveInstance(
                abstractInstanceSyncHandler, instanceInfoDTO, deploymentSummaryDTO, infrastructureMappingDTO));

        // check if existing instance sync perpetual task info record exists or not for incoming infrastructure mapping
        Optional<InstanceSyncPerpetualTaskInfoDTO> instanceSyncPerpetualTaskInfoDTOOptional =
            instanceSyncPerpetualTaskInfoService.findByInfrastructureMappingId(infrastructureMappingDTO.getId());
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
          if (isNewDeploymentInfo(deploymentSummaryDTO.getDeploymentInfoDTO(),
                  instanceSyncPerpetualTaskInfoDTO.getDeploymentInfoDetailsDTOList())) {
            // it means deployment info doesn't exist in the perpetual task info
            // add the deploymentinfo and deployment summary id to the instance sync pt info record
            addNewDeploymentInfoToInstanceSyncPerpetualTaskInfoRecord(
                instanceSyncPerpetualTaskInfoDTO, deploymentSummaryDTO);
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
        .infrastructureMappingId(deploymentSummaryDTO.getInfrastructureMappingId())
        .deploymentInfoDetailsDTOList(
            Collections.singletonList(DeploymentInfoDetailsDTO.builder()
                                          .deploymentInfoDTO(deploymentSummaryDTO.getDeploymentInfoDTO())
                                          .deploymentSummaryId(deploymentSummaryDTO.getId())
                                          .lastUsedAt(System.currentTimeMillis())
                                          .build()))
        .perpetualTaskId(perpetualTaskId)
        .build();
  }

  // Check if the incoming new deployment info is already part of instance sync
  private boolean isNewDeploymentInfo(
      DeploymentInfoDTO newDeploymentInfoDTO, List<DeploymentInfoDetailsDTO> deploymentInfoDetailsDTOList) {
    List<DeploymentInfoDTO> existingDeploymentInfoDTOList = getDeploymentInfoDTOList(deploymentInfoDetailsDTOList);
    return !existingDeploymentInfoDTOList.contains(newDeploymentInfoDTO);
  }

  private List<DeploymentInfoDTO> getDeploymentInfoDTOList(
      List<DeploymentInfoDetailsDTO> deploymentInfoDetailsDTOList) {
    List<DeploymentInfoDTO> deploymentInfoDTOList = new ArrayList<>();
    deploymentInfoDetailsDTOList.forEach(
        deploymentInfoDetailsDTO -> deploymentInfoDTOList.add(deploymentInfoDetailsDTO.getDeploymentInfoDTO()));
    return deploymentInfoDTOList;
  }

  private void addNewDeploymentInfoToInstanceSyncPerpetualTaskInfoRecord(
      InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO, DeploymentSummaryDTO deploymentSummaryDTO) {
    instanceSyncPerpetualTaskInfoDTO.getDeploymentInfoDetailsDTOList().add(
        DeploymentInfoDetailsDTO.builder()
            .deploymentInfoDTO(deploymentSummaryDTO.getDeploymentInfoDTO())
            .deploymentSummaryId(deploymentSummaryDTO.getId())
            .lastUsedAt(System.currentTimeMillis())
            .build());
  }

  private InstanceDTO buildInstance(AbstractInstanceSyncHandler abstractInstanceSyncHandler,
      InstanceInfoDTO instanceInfoDTO, DeploymentSummaryDTO deploymentSummaryDTO,
      InfrastructureMappingDTO infrastructureMappingDTO) {
    ServiceEntity serviceEntity = fetchService(infrastructureMappingDTO);
    Environment environment = fetchEnvironment(infrastructureMappingDTO);

    // TODO add handling for AUTO SCALED instances
    return InstanceDTO.builder()
        .accountIdentifier(deploymentSummaryDTO.getAccountIdentifier())
        .orgIdentifier(deploymentSummaryDTO.getOrgIdentifier())
        .envId(environment.getId())
        .envType(environment.getType())
        .envName(environment.getName())
        .serviceName(serviceEntity.getName())
        .serviceId(serviceEntity.getId())
        .projectIdentifier(deploymentSummaryDTO.getProjectIdentifier())
        .infrastructureMappingId(infrastructureMappingDTO.getId())
        .instanceType(abstractInstanceSyncHandler.getInstanceType())
        .instanceKey(abstractInstanceSyncHandler.getInstanceKey(instanceInfoDTO))
        .primaryArtifact(deploymentSummaryDTO.getArtifactDetails())
        .infraMappingType(infrastructureMappingDTO.getInfrastructureMappingType())
        .connectorRef(infrastructureMappingDTO.getConnectorRef())
        .lastPipelineExecutionName(deploymentSummaryDTO.getPipelineExecutionName())
        .lastDeployedByName(deploymentSummaryDTO.getDeployedByName())
        .lastPipelineExecutionId(deploymentSummaryDTO.getPipelineExecutionId())
        .lastDeployedById(deploymentSummaryDTO.getDeployedById())
        .lastDeployedAt(deploymentSummaryDTO.getDeployedAt())
        .instanceInfoDTO(instanceInfoDTO)
        .build();
  }

  private void buildAndSaveInstance(AbstractInstanceSyncHandler abstractInstanceSyncHandler,
      InstanceInfoDTO instanceInfoDTO, DeploymentSummaryDTO deploymentSummaryDTO,
      InfrastructureMappingDTO infrastructureMappingDTO) {
    InstanceDTO instanceDTO =
        buildInstance(abstractInstanceSyncHandler, instanceInfoDTO, deploymentSummaryDTO, infrastructureMappingDTO);
    instanceService.saveOrReturnEmptyIfAlreadyExists(instanceDTO);
  }

  private ServiceEntity fetchService(InfrastructureMappingDTO infrastructureMappingDTO) {
    Optional<ServiceEntity> serviceEntityOptional = serviceEntityService.get(
        infrastructureMappingDTO.getAccountIdentifier(), infrastructureMappingDTO.getOrgIdentifier(),
        infrastructureMappingDTO.getProjectIdentifier(), infrastructureMappingDTO.getServiceIdentifier(), false);
    if (!serviceEntityOptional.isPresent()) {
      throw new InvalidRequestException(
          "Service not found for serviceId : {}" + infrastructureMappingDTO.getServiceIdentifier());
    }
    return serviceEntityOptional.get();
  }

  private Environment fetchEnvironment(InfrastructureMappingDTO infrastructureMappingDTO) {
    Optional<Environment> environmentServiceOptional = environmentService.get(
        infrastructureMappingDTO.getAccountIdentifier(), infrastructureMappingDTO.getOrgIdentifier(),
        infrastructureMappingDTO.getProjectIdentifier(), infrastructureMappingDTO.getEnvIdentifier(), false);
    if (!environmentServiceOptional.isPresent()) {
      throw new InvalidRequestException(
          "Environment not found for envId : {}" + infrastructureMappingDTO.getEnvIdentifier());
    }
    return environmentServiceOptional.get();
  }
}
