/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
