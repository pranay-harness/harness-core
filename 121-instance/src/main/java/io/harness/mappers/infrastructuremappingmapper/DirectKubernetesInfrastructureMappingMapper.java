package io.harness.mappers.infrastructuremappingmapper;

import io.harness.dtos.infrastructuremapping.DirectKubernetesInfrastructureMappingDTO;
import io.harness.entities.infrastructureMapping.DirectKubernetesInfrastructureMapping;

import lombok.experimental.UtilityClass;

@io.harness.annotations.dev.OwnedBy(io.harness.annotations.dev.HarnessTeam.DX)
@UtilityClass
public class DirectKubernetesInfrastructureMappingMapper {
  public DirectKubernetesInfrastructureMappingDTO toDTO(
      DirectKubernetesInfrastructureMapping directKubernetesInfrastructureMapping) {
    DirectKubernetesInfrastructureMappingDTO directKubernetesInfrastructureMappingDTO =
        new DirectKubernetesInfrastructureMappingDTO();
    directKubernetesInfrastructureMappingDTO.setClusterName(directKubernetesInfrastructureMapping.getClusterName());
    directKubernetesInfrastructureMappingDTO.setNamespace(directKubernetesInfrastructureMapping.getNamespace());
    directKubernetesInfrastructureMappingDTO.setReleaseName(directKubernetesInfrastructureMapping.getReleaseName());
    return directKubernetesInfrastructureMappingDTO;
  }

  public DirectKubernetesInfrastructureMapping toEntity(
      DirectKubernetesInfrastructureMappingDTO directKubernetesInfrastructureMappingDTO) {
    return DirectKubernetesInfrastructureMapping.builder()
        .clusterName(directKubernetesInfrastructureMappingDTO.getClusterName())
        .namespace(directKubernetesInfrastructureMappingDTO.getNamespace())
        .releaseName(directKubernetesInfrastructureMappingDTO.getReleaseName())
        .build();
  }
}
