package io.harness.ng.core.service.mappers;

import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import io.harness.ng.core.service.dto.ServiceRequestDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.ng.core.service.entity.ServiceEntity;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ServiceElementMapper {
  public ServiceEntity toServiceEntity(String accountId, ServiceRequestDTO serviceRequestDTO) {
    return ServiceEntity.builder()
        .identifier(serviceRequestDTO.getIdentifier())
        .accountId(accountId)
        .orgIdentifier(serviceRequestDTO.getOrgIdentifier())
        .projectIdentifier(serviceRequestDTO.getProjectIdentifier())
        .name(serviceRequestDTO.getName())
        .description(serviceRequestDTO.getDescription())
        .tags(convertToList(serviceRequestDTO.getTags()))
        .version(serviceRequestDTO.getVersion())
        .build();
  }

  public ServiceResponseDTO writeDTO(ServiceEntity serviceEntity) {
    return ServiceResponseDTO.builder()
        .accountId(serviceEntity.getAccountId())
        .orgIdentifier(serviceEntity.getOrgIdentifier())
        .projectIdentifier(serviceEntity.getProjectIdentifier())
        .identifier(serviceEntity.getIdentifier())
        .name(serviceEntity.getName())
        .description(serviceEntity.getDescription())
        .deleted(serviceEntity.getDeleted())
        .tags(convertToMap(serviceEntity.getTags()))
        .version(serviceEntity.getVersion())
        .build();
  }
}
