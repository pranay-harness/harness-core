/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InstanceDTO;
import io.harness.models.InstanceDetailsDTO;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;
import io.harness.service.instancesynchandlerfactory.InstanceSyncHandlerFactoryService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceDetailsMapper {
  private final InstanceSyncHandlerFactoryService instanceSyncHandlerFactoryService;

  public List<InstanceDetailsDTO> toInstanceDetailsDTOList(List<InstanceDTO> instanceDTOList) {
    if (instanceDTOList == null) {
      return new ArrayList<>();
    }
    List<InstanceDetailsDTO> instanceDetailsDTOList = new ArrayList<>();
    instanceDTOList.forEach(instanceDTO -> instanceDetailsDTOList.add(toInstanceDetailsDTO(instanceDTO)));
    return instanceDetailsDTOList;
  }

  private InstanceDetailsDTO toInstanceDetailsDTO(InstanceDTO instanceDTO) {
    AbstractInstanceSyncHandler instanceSyncHandler =
        instanceSyncHandlerFactoryService.getInstanceSyncHandler(instanceDTO.getInfrastructureKind());
    return InstanceDetailsDTO.builder()
        .artifactName(instanceDTO.getPrimaryArtifact().getTag())
        .connectorRef(instanceDTO.getConnectorRef())
        .deployedAt(instanceDTO.getLastDeployedAt())
        .deployedById(instanceDTO.getLastDeployedById())
        .deployedByName(instanceDTO.getLastDeployedByName())
        .infrastructureDetails(instanceSyncHandler.getInfrastructureDetails(instanceDTO.getInstanceInfoDTO()))
        .pipelineExecutionName(instanceDTO.getLastPipelineExecutionName())
        .podName(instanceDTO.getInstanceInfoDTO().getPodName())
        // TODO set terraform instance
        .build();
  }
}
