/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.entities.InfrastructureMapping;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class InfrastructureMappingMapper {
  public InfrastructureMappingDTO toDTO(InfrastructureMapping infrastructureMapping) {
    return InfrastructureMappingDTO.builder()
        .accountIdentifier(infrastructureMapping.getAccountIdentifier())
        .orgIdentifier(infrastructureMapping.getOrgIdentifier())
        .projectIdentifier(infrastructureMapping.getProjectIdentifier())
        .connectorRef(infrastructureMapping.getConnectorRef())
        .envIdentifier(infrastructureMapping.getEnvId())
        .id(infrastructureMapping.getId())
        .infrastructureKey(infrastructureMapping.getInfrastructureKey())
        .serviceIdentifier(infrastructureMapping.getServiceId())
        .infrastructureKind(infrastructureMapping.getInfrastructureKind())
        .build();
  }

  public InfrastructureMapping toEntity(InfrastructureMappingDTO infrastructureMappingDTO) {
    return InfrastructureMapping.builder()
        .accountIdentifier(infrastructureMappingDTO.getAccountIdentifier())
        .orgIdentifier(infrastructureMappingDTO.getOrgIdentifier())
        .projectIdentifier(infrastructureMappingDTO.getProjectIdentifier())
        .connectorRef(infrastructureMappingDTO.getConnectorRef())
        .envId(infrastructureMappingDTO.getEnvIdentifier())
        .infrastructureKey(infrastructureMappingDTO.getInfrastructureKey())
        .infrastructureKind(infrastructureMappingDTO.getInfrastructureKind())
        .serviceId(infrastructureMappingDTO.getServiceIdentifier())
        .build();
  }
}
