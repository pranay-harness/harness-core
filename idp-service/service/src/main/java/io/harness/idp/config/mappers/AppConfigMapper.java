/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.config.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.config.beans.dto.AppConfigDTO;
import io.harness.idp.config.beans.entity.AppConfig;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.IDP)
@UtilityClass
public class AppConfigMapper {
  public AppConfigDTO toDTO(AppConfig appConfig) {
    return AppConfigDTO.builder()
        .accountIdentifier(appConfig.getAccountIdentifier())
        .createdAt(appConfig.getCreatedAt())
        .lastModifiedAt(appConfig.getLastModifiedAt())
        .isDeleted(appConfig.isDeleted())
        .deletedAt(appConfig.getDeletedAt())
        .build();
  }

  public AppConfig fromDTO(AppConfigDTO appConfigDTO) {
    return AppConfig.builder()
        .accountIdentifier(appConfigDTO.getAccountIdentifier())
        .createdAt(appConfigDTO.getCreatedAt())
        .lastModifiedAt(appConfigDTO.getLastModifiedAt())
        .isDeleted(appConfigDTO.isDeleted())
        .deletedAt(appConfigDTO.getDeletedAt())
        .build();
  }
}