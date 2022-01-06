/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.entities.ApiKey.DEFAULT_TTL_FOR_TOKEN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ApiKeyDTO;
import io.harness.ng.core.entities.ApiKey;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ApiKeyDTOMapper {
  public ApiKey getApiKeyFromDTO(ApiKeyDTO dto) {
    return ApiKey.builder()
        .identifier(dto.getIdentifier())
        .apiKeyType(dto.getApiKeyType())
        .parentIdentifier(dto.getParentIdentifier())
        .accountIdentifier(dto.getAccountIdentifier())
        .orgIdentifier(dto.getOrgIdentifier())
        .projectIdentifier(dto.getProjectIdentifier())
        .name(dto.getName())
        .description(dto.getDescription())
        .tags(TagMapper.convertToList(dto.getTags()))
        .defaultTimeToExpireToken(
            dto.getDefaultTimeToExpireToken() == null ? DEFAULT_TTL_FOR_TOKEN : dto.getDefaultTimeToExpireToken())
        .build();
  }

  public ApiKeyDTO getDTOFromApiKey(ApiKey apiKey) {
    return ApiKeyDTO.builder()
        .identifier(apiKey.getIdentifier())
        .apiKeyType(apiKey.getApiKeyType())
        .parentIdentifier(apiKey.getParentIdentifier())
        .accountIdentifier(apiKey.getAccountIdentifier())
        .orgIdentifier(apiKey.getOrgIdentifier())
        .projectIdentifier(apiKey.getProjectIdentifier())
        .defaultTimeToExpireToken(apiKey.getDefaultTimeToExpireToken())
        .name(apiKey.getName())
        .description(apiKey.getDescription())
        .tags(TagMapper.convertToMap(apiKey.getTags()))
        .build();
  }
}
