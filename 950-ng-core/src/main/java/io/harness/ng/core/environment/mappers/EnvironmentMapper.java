/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.environment.mappers;

import static io.harness.NGConstants.HARNESS_BLUE;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentBasicInfo;
import io.harness.ng.core.environment.dto.EnvironmentRequestDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class EnvironmentMapper {
  public Environment toEnvironmentEntity(String accountId, EnvironmentRequestDTO environmentRequestDTO) {
    return Environment.builder()
        .identifier(environmentRequestDTO.getIdentifier())
        .accountId(accountId)
        .orgIdentifier(environmentRequestDTO.getOrgIdentifier())
        .projectIdentifier(environmentRequestDTO.getProjectIdentifier())
        .name(environmentRequestDTO.getName())
        .color(Optional.ofNullable(environmentRequestDTO.getColor()).orElse(HARNESS_BLUE))
        .description(environmentRequestDTO.getDescription())
        .type(environmentRequestDTO.getType())
        .tags(convertToList(environmentRequestDTO.getTags()))
        .version(environmentRequestDTO.getVersion())
        .build();
  }

  public EnvironmentResponseDTO writeDTO(Environment environment) {
    return EnvironmentResponseDTO.builder()
        .accountId(environment.getAccountId())
        .orgIdentifier(environment.getOrgIdentifier())
        .projectIdentifier(environment.getProjectIdentifier())
        .identifier(environment.getIdentifier())
        .name(environment.getName())
        .color(Optional.ofNullable(environment.getColor()).orElse(HARNESS_BLUE))
        .description(environment.getDescription())
        .type(environment.getType())
        .deleted(environment.getDeleted())
        .tags(convertToMap(environment.getTags()))
        .version(environment.getVersion())
        .build();
  }

  public EnvironmentResponse toResponseWrapper(Environment environment) {
    return EnvironmentResponse.builder()
        .environment(writeDTO(environment))
        .createdAt(environment.getCreatedAt())
        .lastModifiedAt(environment.getLastModifiedAt())
        .build();
  }

  public EnvironmentBasicInfo toBasicInfo(Environment environment) {
    return EnvironmentBasicInfo.builder()
        .identifier(environment.getIdentifier())
        .name(environment.getName())
        .description(environment.getDescription())
        .type(environment.getType())
        .accountIdentifier(environment.getAccountId())
        .orgIdentifier(environment.getOrgIdentifier())
        .projectIdentifier(environment.getProjectIdentifier())
        .tags(convertToMap(environment.getTags()))
        .color(environment.getColor())
        .build();
  }
}
