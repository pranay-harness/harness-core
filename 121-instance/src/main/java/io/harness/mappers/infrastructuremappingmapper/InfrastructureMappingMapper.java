package io.harness.mappers.infrastructuremappingmapper;

import io.harness.dtos.infrastructuremapping.DirectKubernetesInfrastructureMappingDTO;
import io.harness.dtos.infrastructuremapping.InfrastructureMappingDTO;
import io.harness.entities.infrastructureMapping.DirectKubernetesInfrastructureMapping;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@io.harness.annotations.dev.OwnedBy(io.harness.annotations.dev.HarnessTeam.DX)
@UtilityClass
public class InfrastructureMappingMapper {
  public InfrastructureMappingDTO toDTO(InfrastructureMapping infrastructureMapping) {
    InfrastructureMappingDTO infrastructureMappingDTO = null;
    if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping) {
      infrastructureMappingDTO = DirectKubernetesInfrastructureMappingMapper.toDTO(
          (DirectKubernetesInfrastructureMapping) infrastructureMapping);
    } else {
      throw new InvalidRequestException(
          "No InfrastructureMappingMapper toDTO found for infrastructureMapping : {}" + infrastructureMapping);
    }

    infrastructureMappingDTO.setAccountIdentifier(infrastructureMapping.getAccountIdentifier());
    infrastructureMappingDTO.setConnectorRef(infrastructureMapping.getConnectorRef());
    infrastructureMappingDTO.setOrgIdentifier(infrastructureMapping.getOrgIdentifier());
    infrastructureMappingDTO.setProjectIdentifier(infrastructureMapping.getProjectIdentifier());
    infrastructureMappingDTO.setDeploymentType(infrastructureMapping.getDeploymentType());
    infrastructureMappingDTO.setEnvId(infrastructureMapping.getEnvId());
    infrastructureMappingDTO.setId(infrastructureMapping.getId());
    infrastructureMappingDTO.setInfrastructureMappingType(infrastructureMapping.getInfrastructureMappingType());
    infrastructureMappingDTO.setServiceId(infrastructureMapping.getServiceId());
    return infrastructureMappingDTO;
  }

  public InfrastructureMapping toEntity(InfrastructureMappingDTO infrastructureMappingDTO) {
    InfrastructureMapping infrastructureMapping = null;
    if (infrastructureMappingDTO instanceof DirectKubernetesInfrastructureMappingDTO) {
      infrastructureMapping = DirectKubernetesInfrastructureMappingMapper.toEntity(
          (DirectKubernetesInfrastructureMappingDTO) infrastructureMappingDTO);
    } else {
      throw new InvalidRequestException(
          "No InfrastructureMappingMapper toEntity found for infrastructureMappingDTO : {}" + infrastructureMappingDTO);
    }

    infrastructureMapping.setAccountIdentifier(infrastructureMappingDTO.getAccountIdentifier());
    infrastructureMapping.setConnectorRef(infrastructureMappingDTO.getConnectorRef());
    infrastructureMapping.setOrgIdentifier(infrastructureMappingDTO.getOrgIdentifier());
    infrastructureMapping.setProjectIdentifier(infrastructureMappingDTO.getProjectIdentifier());
    infrastructureMapping.setDeploymentType(infrastructureMappingDTO.getDeploymentType());
    infrastructureMapping.setEnvId(infrastructureMappingDTO.getEnvId());
    infrastructureMapping.setId(infrastructureMappingDTO.getId());
    infrastructureMapping.setInfrastructureMappingType(infrastructureMappingDTO.getInfrastructureMappingType());
    infrastructureMapping.setServiceId(infrastructureMappingDTO.getServiceId());
    return infrastructureMapping;
  }
}
