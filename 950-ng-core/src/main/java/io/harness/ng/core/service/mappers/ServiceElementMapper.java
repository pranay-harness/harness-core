/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.service.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.service.dto.ServiceRequestDTO;
import io.harness.ng.core.service.dto.ServiceResponse;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.ng.core.service.entity.ServiceBasicInfo;
import io.harness.ng.core.service.entity.ServiceEntity;

import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
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
  public ServiceResponse toResponseWrapper(ServiceEntity serviceEntity) {
    return ServiceResponse.builder()
        .service(writeDTO(serviceEntity))
        .createdAt(serviceEntity.getCreatedAt())
        .lastModifiedAt(serviceEntity.getLastModifiedAt())
        .build();
  }

  public ServiceBasicInfo toBasicInfo(ServiceEntity serviceEntity) {
    return ServiceBasicInfo.builder()
        .identifier(serviceEntity.getIdentifier())
        .name(serviceEntity.getName())
        .description(serviceEntity.getDescription())
        .accountIdentifier(serviceEntity.getAccountId())
        .orgIdentifier(serviceEntity.getOrgIdentifier())
        .projectIdentifier(serviceEntity.getProjectIdentifier())
        .tags(convertToMap(serviceEntity.getTags()))
        .build();
  }
}
