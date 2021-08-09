package io.harness.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InstanceDTO;
import io.harness.entities.Instance;
import io.harness.mappers.instanceinfo.InstanceInfoMapper;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class InstanceMapper {
  public InstanceDTO toDTO(Instance instance) {
    return InstanceDTO.builder()
        .accountIdentifier(instance.getAccountIdentifier())
        .envId(instance.getEnvId())
        .envName(instance.getEnvName())
        .envType(instance.getEnvType())
        .infrastructureKind(instance.getInfrastructureKind())
        .infrastructureMappingId(instance.getInfrastructureMappingId())
        .instanceType(instance.getInstanceType())
        .lastDeployedAt(instance.getLastDeployedAt())
        .lastDeployedById(instance.getLastDeployedById())
        .lastDeployedByName(instance.getLastDeployedByName())
        .lastPipelineExecutionId(instance.getLastPipelineExecutionId())
        .lastPipelineExecutionName(instance.getLastPipelineExecutionName())
        .orgIdentifier(instance.getOrgIdentifier())
        .projectIdentifier(instance.getProjectIdentifier())
        .primaryArtifact(instance.getPrimaryArtifact())
        .serviceId(instance.getServiceId())
        .serviceName(instance.getServiceName())
        .createdAt(instance.getCreatedAt())
        .deletedAt(instance.getDeletedAt())
        .isDeleted(instance.isDeleted())
        .lastModifiedAt(instance.getLastModifiedAt())
        .connectorRef(instance.getConnectorRef())
        .instanceInfoDTO(InstanceInfoMapper.toDTO(instance.getInstanceInfo()))
        .instanceKey(instance.getInstanceKey())
        .build();
  }

  public List<InstanceDTO> toDTO(List<Instance> instances) {
    List<InstanceDTO> instanceDTOList = new ArrayList<>();
    instances.forEach(instance -> instanceDTOList.add(toDTO(instance)));
    return instanceDTOList;
  }

  public Instance toEntity(InstanceDTO instanceDTO) {
    return Instance.builder()
        .accountIdentifier(instanceDTO.getAccountIdentifier())
        .envId(instanceDTO.getEnvId())
        .envName(instanceDTO.getEnvName())
        .envType(instanceDTO.getEnvType())
        .infrastructureKind(instanceDTO.getInfrastructureKind())
        .infrastructureMappingId(instanceDTO.getInfrastructureMappingId())
        .instanceType(instanceDTO.getInstanceType())
        .lastDeployedAt(instanceDTO.getLastDeployedAt())
        .lastDeployedById(instanceDTO.getLastDeployedById())
        .lastDeployedByName(instanceDTO.getLastDeployedByName())
        .lastPipelineExecutionId(instanceDTO.getLastPipelineExecutionId())
        .lastPipelineExecutionName(instanceDTO.getLastPipelineExecutionName())
        .orgIdentifier(instanceDTO.getOrgIdentifier())
        .projectIdentifier(instanceDTO.getProjectIdentifier())
        .primaryArtifact(instanceDTO.getPrimaryArtifact())
        .serviceId(instanceDTO.getServiceId())
        .serviceName(instanceDTO.getServiceName())
        .createdAt(instanceDTO.getCreatedAt())
        .deletedAt(instanceDTO.getDeletedAt())
        .isDeleted(instanceDTO.isDeleted())
        .lastModifiedAt(instanceDTO.getLastModifiedAt())
        .connectorRef(instanceDTO.getConnectorRef())
        .instanceInfo(InstanceInfoMapper.toEntity(instanceDTO.getInstanceInfoDTO()))
        .instanceKey(instanceDTO.getInstanceKey())
        .build();
  }
}
